package com.yan.campuspass.activity.service;

import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.domain.ActivityStatus;
import com.yan.campuspass.activity.dto.ActivityResponse;
import com.yan.campuspass.activity.dto.CreateActivityRequest;
import com.yan.campuspass.activity.mapper.ActivityMapper;
import com.yan.campuspass.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    private ActivityMapper activityMapper;
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-08-01T01:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        activityService = new ActivityService(activityMapper, fixedClock);
    }

    @Test
    void shouldCreateActivityAsDraft() {
        when(activityMapper.insert(org.mockito.ArgumentMatchers.any(Activity.class)))
                .thenAnswer(invocation -> {
                    Activity activity = invocation.getArgument(0);
                    activity.setId(1L);
                    return 1;
                });

        CreateActivityRequest request = new CreateActivityRequest(
                "Java技术分享会",
                "讲座",
                "图书馆报告厅",
                "介绍JVM与并发编程",
                100,
                LocalDateTime.of(2026, 8, 2, 9, 0),
                LocalDateTime.of(2026, 8, 5, 18, 0),
                LocalDateTime.of(2026, 8, 6, 14, 0),
                LocalDateTime.of(2026, 8, 6, 16, 0)
        );

        ActivityResponse response = activityService.createDraft(1001L, request);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        org.mockito.Mockito.verify(activityMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ActivityStatus.DRAFT);
        assertThat(captor.getValue().getRegisteredCount()).isZero();
        assertThat(captor.getValue().getVersion()).isZero();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldKeepOngoingActivityPubliclyVisible() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus(ActivityStatus.ONGOING);
        when(activityMapper.selectById(10L)).thenReturn(activity);

        ActivityResponse response =
                activityService.getPublishedActivity(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status())
                .isEqualTo(ActivityStatus.ONGOING);
    }

    @Test
    void shouldHideDraftFromPublicDetail() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus(ActivityStatus.DRAFT);
        when(activityMapper.selectById(10L)).thenReturn(activity);

        assertThatThrownBy(
                () -> activityService.getPublishedActivity(10L)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动不存在或尚未发布");
    }
}
