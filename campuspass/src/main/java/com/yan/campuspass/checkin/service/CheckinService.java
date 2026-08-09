package com.yan.campuspass.checkin.service;

import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.checkin.domain.ActivityCheckin;
import com.yan.campuspass.checkin.dto.CheckinResponse;
import com.yan.campuspass.checkin.dto.CheckinTokenResponse;
import com.yan.campuspass.checkin.mapper.CheckinMapper;
import com.yan.campuspass.common.exception.BusinessException;
import com.yan.campuspass.registration.mapper.RegistrationMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CheckinService {

    private static final String KEY_PREFIX = "checkin:token:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private static final Duration CHECKIN_EARLY_WINDOW =
            Duration.ofMinutes(30);

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckinMapper checkinMapper;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public CheckinService(
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            CheckinMapper checkinMapper,
            StringRedisTemplate redisTemplate,
            Clock clock) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.checkinMapper = checkinMapper;
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public CheckinTokenResponse generateToken(
            Long organizerId,
            Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null
                || !activity.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("只能为自己的活动生成签到码");
        }
        validateCheckinTime(activity, LocalDateTime.now(clock));

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                String.valueOf(activityId),
                TOKEN_TTL
        );
        return new CheckinTokenResponse(
                token,
                clock.instant().plus(TOKEN_TTL)
        );
    }

    @Transactional
    public CheckinResponse checkIn(Long userId, String token) {
        String activityIdValue = redisTemplate.opsForValue()
                .get(KEY_PREFIX + token);
        if (activityIdValue == null) {
            throw new BusinessException("签到码无效或已经过期");
        }

        Long activityId = Long.valueOf(activityIdValue);
        LocalDateTime now = LocalDateTime.now(clock);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("签到活动不存在");
        }
        validateCheckinTime(activity, now);

        int updatedRows = registrationMapper.checkIn(
                activityId,
                userId,
                now
        );
        if (updatedRows != 1) {
            throw new BusinessException(
                    "只有预约成功且未签到的学生可以签到"
            );
        }

        ActivityCheckin checkin = new ActivityCheckin();
        checkin.setActivityId(activityId);
        checkin.setUserId(userId);
        checkin.setCheckedAt(now);
        checkinMapper.insert(checkin);

        return new CheckinResponse(activityId, userId, now);
    }

    private void validateCheckinTime(
            Activity activity,
            LocalDateTime now) {
        LocalDateTime checkinStart = activity.getActivityStartTime()
                .minus(CHECKIN_EARLY_WINDOW);
        if (now.isBefore(checkinStart)) {
            throw new BusinessException("活动开始前30分钟才能签到");
        }
        if (now.isAfter(activity.getActivityEndTime())) {
            throw new BusinessException("活动已经结束，不能签到");
        }
    }
}
