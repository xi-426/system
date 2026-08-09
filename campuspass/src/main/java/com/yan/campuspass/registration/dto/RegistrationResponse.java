package com.yan.campuspass.registration.dto;

import com.yan.campuspass.registration.domain.ActivityRegistration;
import com.yan.campuspass.registration.domain.RegistrationStatus;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long registrationId,
        Long activityId,
        Long userId,
        RegistrationStatus status,
        LocalDateTime reservedAt
) {
    public static RegistrationResponse from(ActivityRegistration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getActivityId(),
                registration.getUserId(),
                registration.getStatus(),
                registration.getReservedAt()
        );
    }
}
