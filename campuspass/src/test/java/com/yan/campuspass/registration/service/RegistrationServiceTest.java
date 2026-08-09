package com.yan.campuspass.registration.service;

import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.domain.ActivityStatus;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.common.exception.BusinessException;
import com.yan.campuspass.notification.event.NotificationEventPublisher;
import com.yan.campuspass.registration.domain.ActivityRegistration;
import com.yan.campuspass.registration.domain.RegistrationStatus;
import com.yan.campuspass.registration.dto.ReservationOutcome;
import com.yan.campuspass.registration.dto.ReservationResultResponse;
import com.yan.campuspass.registration.dto.RegistrationResponse;
import com.yan.campuspass.registration.mapper.RegistrationMapper;
import com.yan.campuspass.waitlist.domain.ActivityWaitlist;
import com.yan.campuspass.waitlist.domain.WaitlistStatus;
import com.yan.campuspass.waitlist.mapper.WaitlistMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private ActivityMapper activityMapper;
    private RegistrationMapper registrationMapper;
    private WaitlistMapper waitlistMapper;
    private NotificationEventPublisher notificationPublisher;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        registrationMapper = mock(RegistrationMapper.class);
        waitlistMapper = mock(WaitlistMapper.class);
        notificationPublisher = mock(NotificationEventPublisher.class);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-07-30T02:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        registrationService = new RegistrationService(
                activityMapper,
                registrationMapper,
                waitlistMapper,
                notificationPublisher,
                fixedClock
        );
    }

    @Test
    void shouldReserveWhenSeatIsAvailable() {
        when(activityMapper.selectById(10L)).thenReturn(openActivity());
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(activityMapper.reserveSeat(any(), any())).thenReturn(1);
        when(registrationMapper.insert(any(ActivityRegistration.class)))
                .thenAnswer(invocation -> {
            ActivityRegistration registration = invocation.getArgument(0);
            registration.setId(100L);
            return 1;
        });

        ReservationResultResponse response =
                registrationService.reserve(2L, 10L);

        assertThat(response.registrationId()).isEqualTo(100L);
        assertThat(response.outcome()).isEqualTo(ReservationOutcome.RESERVED);
        verify(activityMapper).reserveSeat(any(), any());
        verify(registrationMapper).insert(any(ActivityRegistration.class));
    }

    @Test
    void shouldRejectDuplicateRegistrationBeforeTakingSeat() {
        when(activityMapper.selectById(10L)).thenReturn(openActivity());
        when(registrationMapper.selectOne(any()))
                .thenReturn(registration(100L, RegistrationStatus.RESERVED));

        assertThatThrownBy(() -> registrationService.reserve(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请勿重复预约同一活动");

        verify(activityMapper, never()).reserveSeat(any(), any());
        verify(registrationMapper, never())
                .insert(any(ActivityRegistration.class));
    }

    @Test
    void shouldJoinWaitlistWhenNoSeatCanBeTaken() {
        when(activityMapper.selectById(10L)).thenReturn(openActivity());
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(activityMapper.reserveSeat(any(), any())).thenReturn(0);
        when(waitlistMapper.insert(any(ActivityWaitlist.class)))
                .thenAnswer(invocation -> {
                    ActivityWaitlist waitlist = invocation.getArgument(0);
                    waitlist.setId(200L);
                    return 1;
                });
        when(waitlistMapper.countWaiting(10L)).thenReturn(1);

        ReservationResultResponse response =
                registrationService.reserve(2L, 10L);

        assertThat(response.outcome()).isEqualTo(ReservationOutcome.WAITLISTED);
        assertThat(response.waitlistId()).isEqualTo(200L);
        assertThat(response.waitlistPosition()).isEqualTo(1);
        verify(registrationMapper, never())
                .insert(any(ActivityRegistration.class));
    }

    @Test
    void shouldCancelReservationAndReleaseSeat() {
        ActivityRegistration reserved = registration(
                100L,
                RegistrationStatus.RESERVED
        );
        ActivityRegistration cancelled = registration(
                100L,
                RegistrationStatus.CANCELLED
        );
        when(activityMapper.selectById(10L)).thenReturn(openActivity());
        when(registrationMapper.selectOne(any())).thenReturn(reserved);
        when(registrationMapper.cancel(any(), any(), any())).thenReturn(1);
        when(activityMapper.releaseSeat(any(), any())).thenReturn(1);
        when(registrationMapper.selectById(100L)).thenReturn(cancelled);

        RegistrationResponse response = registrationService.cancel(2L, 10L);

        assertThat(response.status()).isEqualTo(RegistrationStatus.CANCELLED);
        verify(registrationMapper).cancel(any(), any(), any());
        verify(activityMapper).releaseSeat(any(), any());
    }

    @Test
    void shouldRejectRepeatedCancellation() {
        ActivityRegistration cancelled = registration(
                100L,
                RegistrationStatus.CANCELLED
        );
        when(activityMapper.selectById(10L)).thenReturn(openActivity());
        when(registrationMapper.selectOne(any())).thenReturn(cancelled);

        assertThatThrownBy(() -> registrationService.cancel(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前没有可取消的预约");

        verify(registrationMapper, never()).cancel(any(), any(), any());
        verify(activityMapper, never()).releaseSeat(any(), any());
    }

    @Test
    void shouldReactivateCancelledRegistration() {
        ActivityRegistration cancelled = registration(
                100L,
                RegistrationStatus.CANCELLED
        );
        ActivityRegistration reserved = registration(
                100L,
                RegistrationStatus.RESERVED
        );
        when(activityMapper.selectById(10L)).thenReturn(openActivity());
        when(registrationMapper.selectOne(any())).thenReturn(cancelled);
        when(activityMapper.reserveSeat(any(), any())).thenReturn(1);
        when(registrationMapper.reactivate(any(), any(), any())).thenReturn(1);
        when(registrationMapper.selectById(100L)).thenReturn(reserved);

        ReservationResultResponse response =
                registrationService.reserve(2L, 10L);

        assertThat(response.outcome()).isEqualTo(ReservationOutcome.RESERVED);
        verify(activityMapper).reserveSeat(any(), any());
        verify(registrationMapper).reactivate(any(), any(), any());
        verify(registrationMapper, never())
                .insert(any(ActivityRegistration.class));
    }

    @Test
    void shouldPromoteFirstWaitlistWhenReservationIsCancelled() {
        ActivityRegistration reserved = registration(
                100L,
                RegistrationStatus.RESERVED
        );
        ActivityWaitlist firstWaiting = new ActivityWaitlist();
        firstWaiting.setId(200L);
        firstWaiting.setActivityId(10L);
        firstWaiting.setUserId(3L);
        firstWaiting.setStatus(WaitlistStatus.WAITING);

        when(activityMapper.selectById(10L)).thenReturn(openActivity());
        when(registrationMapper.selectOne(any()))
                .thenReturn(reserved)
                .thenReturn(null);
        when(registrationMapper.cancel(any(), any(), any())).thenReturn(1);
        when(waitlistMapper.selectFirstWaitingForUpdate(10L))
                .thenReturn(firstWaiting);
        when(waitlistMapper.promote(eq(200L), any())).thenReturn(1);
        when(registrationMapper.insert(any(ActivityRegistration.class)))
                .thenReturn(1);
        when(registrationMapper.selectById(100L))
                .thenReturn(registration(100L, RegistrationStatus.CANCELLED));

        registrationService.cancel(2L, 10L);

        verify(waitlistMapper).promote(eq(200L), any());
        verify(activityMapper, never()).releaseSeat(any(), any());
        verify(registrationMapper).insert(any(ActivityRegistration.class));
    }

    private Activity openActivity() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setRegistrationStartTime(
                LocalDateTime.of(2026, 7, 29, 9, 0)
        );
        activity.setRegistrationEndTime(
                LocalDateTime.of(2026, 8, 30, 18, 0)
        );
        activity.setActivityStartTime(
                LocalDateTime.of(2026, 8, 31, 14, 0)
        );
        return activity;
    }

    private ActivityRegistration registration(
            Long id,
            RegistrationStatus status) {
        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(id);
        registration.setActivityId(10L);
        registration.setUserId(2L);
        registration.setStatus(status);
        return registration;
    }
}
