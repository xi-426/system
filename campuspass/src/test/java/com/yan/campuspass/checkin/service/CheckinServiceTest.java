package com.yan.campuspass.checkin.service;

import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.checkin.domain.ActivityCheckin;
import com.yan.campuspass.checkin.dto.CheckinResponse;
import com.yan.campuspass.checkin.dto.CheckinTokenResponse;
import com.yan.campuspass.checkin.mapper.CheckinMapper;
import com.yan.campuspass.common.exception.BusinessException;
import com.yan.campuspass.registration.mapper.RegistrationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
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

class CheckinServiceTest {

    private ActivityMapper activityMapper;
    private RegistrationMapper registrationMapper;
    private CheckinMapper checkinMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CheckinService checkinService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        registrationMapper = mock(RegistrationMapper.class);
        checkinMapper = mock(CheckinMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-07-30T02:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        checkinService = new CheckinService(
                activityMapper,
                registrationMapper,
                checkinMapper,
                redisTemplate,
                fixedClock
        );
    }

    @Test
    void shouldGenerateTenMinuteTokenForActivityOwner() {
        Activity activity = checkinOpenActivity();
        when(activityMapper.selectById(10L)).thenReturn(activity);

        CheckinTokenResponse response =
                checkinService.generateToken(1L, 10L);

        assertThat(response.token()).hasSize(32);
        assertThat(response.expiresAt())
                .isEqualTo(Instant.parse("2026-07-30T02:10:00Z"));
        verify(valueOperations).set(
                eq("checkin:token:" + response.token()),
                eq("10"),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void shouldCheckInReservedStudent() {
        when(valueOperations.get("checkin:token:valid-token"))
                .thenReturn("10");
        when(activityMapper.selectById(10L))
                .thenReturn(checkinOpenActivity());
        when(registrationMapper.checkIn(
                eq(10L),
                eq(2L),
                any(LocalDateTime.class)
        )).thenReturn(1);

        CheckinResponse response =
                checkinService.checkIn(2L, "valid-token");

        assertThat(response.activityId()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(2L);
        verify(checkinMapper).insert(any(ActivityCheckin.class));
    }

    @Test
    void shouldRejectTokenGenerationBeforeCheckinWindow() {
        Activity activity = checkinOpenActivity();
        activity.setActivityStartTime(
                LocalDateTime.of(2026, 7, 30, 11, 0)
        );
        when(activityMapper.selectById(10L)).thenReturn(activity);

        assertThatThrownBy(
                () -> checkinService.generateToken(1L, 10L)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动开始前30分钟才能签到");

        verify(valueOperations, never())
                .set(any(), any(), any(Duration.class));
    }

    @Test
    void shouldRejectCheckinAfterActivityEnds() {
        Activity activity = checkinOpenActivity();
        activity.setActivityEndTime(
                LocalDateTime.of(2026, 7, 30, 9, 59)
        );
        when(valueOperations.get("checkin:token:ended"))
                .thenReturn("10");
        when(activityMapper.selectById(10L)).thenReturn(activity);

        assertThatThrownBy(
                () -> checkinService.checkIn(2L, "ended")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动已经结束，不能签到");

        verify(registrationMapper, never())
                .checkIn(any(), any(), any());
    }

    @Test
    void shouldRejectExpiredTokenBeforeUpdatingDatabase() {
        when(valueOperations.get("checkin:token:expired"))
                .thenReturn(null);

        assertThatThrownBy(
                () -> checkinService.checkIn(2L, "expired")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage("签到码无效或已经过期");

        verify(registrationMapper, never())
                .checkIn(any(), any(), any());
        verify(checkinMapper, never())
                .insert(any(ActivityCheckin.class));
    }

    private Activity checkinOpenActivity() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setOrganizerId(1L);
        activity.setActivityStartTime(
                LocalDateTime.of(2026, 7, 30, 10, 20)
        );
        activity.setActivityEndTime(
                LocalDateTime.of(2026, 7, 30, 12, 0)
        );
        return activity;
    }
}
