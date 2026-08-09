package com.yan.campuspass.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.domain.ActivityStatus;
import com.yan.campuspass.activity.dto.ActivityResponse;
import com.yan.campuspass.activity.dto.CreateActivityRequest;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class ActivityService {

    private static final Set<ActivityStatus> PUBLIC_ACTIVITY_STATUSES =
            EnumSet.of(
                    ActivityStatus.PUBLISHED,
                    ActivityStatus.REGISTRATION_CLOSED,
                    ActivityStatus.ONGOING,
                    ActivityStatus.FINISHED
            );

    private final ActivityMapper activityMapper;
    private final Clock clock;

    public ActivityService(ActivityMapper activityMapper, Clock clock) {
        this.activityMapper = activityMapper;
        this.clock = clock;
    }

    @Transactional
    public ActivityResponse createDraft(Long organizerId, CreateActivityRequest request) {
        validateTimeOrder(request);

        LocalDateTime now = LocalDateTime.now(clock);
        Activity activity = new Activity();
        activity.setOrganizerId(organizerId);
        activity.setTitle(request.title());
        activity.setCategory(request.category());
        activity.setLocation(request.location());
        activity.setDescription(request.description());
        activity.setCapacity(request.capacity());
        activity.setRegisteredCount(0);
        activity.setRegistrationStartTime(request.registrationStartTime());
        activity.setRegistrationEndTime(request.registrationEndTime());
        activity.setActivityStartTime(request.activityStartTime());
        activity.setActivityEndTime(request.activityEndTime());
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setVersion(0);
        activity.setReminderSent(false);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);

        activityMapper.insert(activity);
        return ActivityResponse.from(activity);
    }

    @Transactional
    public ActivityResponse publish(Long organizerId, Long activityId) {
        Activity activity = getActivity(activityId);
        if (!activity.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("只能发布自己创建的活动");
        }
        if (activity.getStatus() != ActivityStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的活动可以发布");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!activity.getRegistrationEndTime().isAfter(now)) {
            throw new BusinessException("报名已经截止，活动不能发布");
        }

        int affectedRows = activityMapper.publish(activityId, organizerId, now);
        if (affectedRows != 1) {
            throw new BusinessException("活动状态已经变化，请刷新后重试");
        }
        return ActivityResponse.from(getActivity(activityId));
    }

    public ActivityResponse getPublishedActivity(Long activityId) {
        Activity activity = getActivity(activityId);
        if (!PUBLIC_ACTIVITY_STATUSES.contains(activity.getStatus())) {
            throw new BusinessException("活动不存在或尚未发布");
        }
        return ActivityResponse.from(activity);
    }

    public List<ActivityResponse> listPublishedActivities() {
        LambdaQueryWrapper<Activity> query = new LambdaQueryWrapper<Activity>()
                .in(Activity::getStatus, PUBLIC_ACTIVITY_STATUSES)
                .orderByAsc(Activity::getActivityStartTime);
        return activityMapper.selectList(query).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    public List<ActivityResponse> listOrganizerActivities(Long organizerId) {
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOrganizerId, organizerId)
                .orderByDesc(Activity::getCreatedAt)).stream()
                .map(ActivityResponse::from).toList();
    }

    private Activity getActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private void validateTimeOrder(CreateActivityRequest request) {
        if (!request.registrationStartTime().isBefore(request.registrationEndTime())) {
            throw new BusinessException("报名开始时间必须早于报名结束时间");
        }
        if (!request.registrationEndTime().isBefore(request.activityStartTime())) {
            throw new BusinessException("报名结束时间必须早于活动开始时间");
        }
        if (!request.activityStartTime().isBefore(request.activityEndTime())) {
            throw new BusinessException("活动开始时间必须早于活动结束时间");
        }
    }
}
