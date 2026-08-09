package com.yan.campuspass.activity.scheduler;

import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.notification.event.NotificationEventPublisher;
import com.yan.campuspass.registration.mapper.RegistrationMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class ActivityLifecycleScheduler {

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final NotificationEventPublisher notificationPublisher;
    private final Clock clock;

    public ActivityLifecycleScheduler(
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            NotificationEventPublisher notificationPublisher,
            Clock clock) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.notificationPublisher = notificationPublisher;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.lifecycle-cron:0 * * * * *}")
    @Transactional
    public void updateLifecycle() {
        LocalDateTime now = LocalDateTime.now(clock);

        for (Activity activity
                : activityMapper.selectNeedingReminder(
                        now,
                        now.plusHours(1))) {
            int claimedRows = activityMapper.markReminderSent(
                    activity.getId(),
                    now
            );
            if (claimedRows != 1) {
                continue;
            }

            for (Long userId
                    : registrationMapper.selectReservedUserIds(
                            activity.getId())) {
                notificationPublisher.publish(
                        userId,
                        "REMINDER",
                        "活动即将开始",
                        activity.getTitle() + " 将在一小时内开始"
                );
            }
        }

        activityMapper.closeRegistrations(now);
        activityMapper.startActivities(now);
        activityMapper.finishActivities(now);
        registrationMapper.markAbsences(now);
    }
}
