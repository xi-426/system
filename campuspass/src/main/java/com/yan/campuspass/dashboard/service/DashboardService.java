package com.yan.campuspass.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.checkin.domain.ActivityCheckin;
import com.yan.campuspass.checkin.mapper.CheckinMapper;
import com.yan.campuspass.common.exception.BusinessException;
import com.yan.campuspass.dashboard.dto.ActivityStatsResponse;
import com.yan.campuspass.dashboard.dto.NotificationResponse;
import com.yan.campuspass.notification.domain.Notification;
import com.yan.campuspass.notification.mapper.NotificationMapper;
import com.yan.campuspass.registration.domain.ActivityRegistration;
import com.yan.campuspass.registration.mapper.RegistrationMapper;
import com.yan.campuspass.waitlist.domain.ActivityWaitlist;
import com.yan.campuspass.waitlist.mapper.WaitlistMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final RegistrationMapper registrationMapper;
    private final WaitlistMapper waitlistMapper;
    private final NotificationMapper notificationMapper;
    private final ActivityMapper activityMapper;
    private final CheckinMapper checkinMapper;

    public DashboardService(
            RegistrationMapper registrationMapper,
            WaitlistMapper waitlistMapper,
            NotificationMapper notificationMapper,
            ActivityMapper activityMapper,
            CheckinMapper checkinMapper) {
        this.registrationMapper = registrationMapper;
        this.waitlistMapper = waitlistMapper;
        this.notificationMapper = notificationMapper;
        this.activityMapper = activityMapper;
        this.checkinMapper = checkinMapper;
    }

    public List<ActivityRegistration> registrations(Long userId) {
        return registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, userId)
                        .orderByDesc(ActivityRegistration::getUpdatedAt)
        );
    }

    public List<ActivityWaitlist> waitlists(Long userId) {
        return waitlistMapper.selectList(
                new LambdaQueryWrapper<ActivityWaitlist>()
                        .eq(ActivityWaitlist::getUserId, userId)
                        .orderByDesc(ActivityWaitlist::getUpdatedAt)
        );
    }

    public List<NotificationResponse> notifications(Long userId) {
        return notificationMapper.selectList(
                        new LambdaQueryWrapper<Notification>()
                                .eq(Notification::getUserId, userId)
                                .orderByDesc(Notification::getCreatedAt)
                )
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public ActivityStatsResponse stats(
            Long organizerId,
            Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null
                || !activity.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("只能查看自己的活动统计");
        }

        long checkedInCount = checkinMapper.selectCount(
                new LambdaQueryWrapper<ActivityCheckin>()
                        .eq(ActivityCheckin::getActivityId, activityId)
        );
        double attendanceRate = activity.getRegisteredCount() == 0
                ? 0.0
                : (double) checkedInCount / activity.getRegisteredCount();

        return new ActivityStatsResponse(
                activityId,
                activity.getCapacity(),
                activity.getRegisteredCount(),
                checkedInCount,
                attendanceRate
        );
    }
}
