package com.yan.campuspass.waitlist.dto;

import com.yan.campuspass.waitlist.domain.ActivityWaitlist;
import com.yan.campuspass.waitlist.domain.WaitlistStatus;

import java.time.LocalDateTime;

public record WaitlistResponse(
        Long waitlistId,
        Long activityId,
        Long userId,
        WaitlistStatus status,
        LocalDateTime joinedAt
) {
    public static WaitlistResponse from(ActivityWaitlist waitlist) {
        return new WaitlistResponse(
                waitlist.getId(),
                waitlist.getActivityId(),
                waitlist.getUserId(),
                waitlist.getStatus(),
                waitlist.getJoinedAt()
        );
    }
}
