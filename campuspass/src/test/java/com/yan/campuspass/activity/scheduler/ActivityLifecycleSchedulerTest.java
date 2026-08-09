package com.yan.campuspass.activity.scheduler;

import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.notification.event.NotificationEventPublisher;
import com.yan.campuspass.registration.mapper.RegistrationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityLifecycleSchedulerTest {

    private ActivityMapper activityMapper;
    private RegistrationMapper registrationMapper;
    private NotificationEventPublisher notificationPublisher;
    private ActivityLifecycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        registrationMapper = mock(RegistrationMapper.class);
        notificationPublisher = mock(NotificationEventPublisher.class);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-07-30T02:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        scheduler = new ActivityLifecycleScheduler(
                activityMapper,
                registrationMapper,
                notificationPublisher,
                fixedClock
        );
    }

    @Test
    void shouldSendReminderOnlyAfterClaimingActivity() {
        Activity claimed = activity(10L, "Java 分享会");
        Activity alreadyClaimed = activity(11L, "并发分享会");
        when(activityMapper.selectNeedingReminder(any(), any()))
                .thenReturn(List.of(claimed, alreadyClaimed));
        when(activityMapper.markReminderSent(10L, now()))
                .thenReturn(1);
        when(activityMapper.markReminderSent(11L, now()))
                .thenReturn(0);
        when(registrationMapper.selectReservedUserIds(10L))
                .thenReturn(List.of(2L, 3L));

        scheduler.updateLifecycle();

        verify(notificationPublisher).publish(
                2L,
                "REMINDER",
                "活动即将开始",
                "Java 分享会 将在一小时内开始"
        );
        verify(notificationPublisher).publish(
                3L,
                "REMINDER",
                "活动即将开始",
                "Java 分享会 将在一小时内开始"
        );
        verify(registrationMapper, never())
                .selectReservedUserIds(11L);
        verify(activityMapper).closeRegistrations(now());
        verify(activityMapper).startActivities(now());
        verify(activityMapper).finishActivities(now());
        verify(registrationMapper).markAbsences(now());
    }

    private Activity activity(Long id, String title) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setTitle(title);
        return activity;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 7, 30, 10, 0);
    }
}
