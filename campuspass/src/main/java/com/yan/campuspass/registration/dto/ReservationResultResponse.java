package com.yan.campuspass.registration.dto;

import java.time.LocalDateTime;

public record ReservationResultResponse(
        ReservationOutcome outcome,
        Long activityId,
        Long userId,
        Long registrationId,
        Long waitlistId,
        Integer waitlistPosition,
        LocalDateTime occurredAt
) {
    public static ReservationResultResponse reserved(
            Long activityId,
            Long userId,
            Long registrationId,
            LocalDateTime occurredAt) {
        return new ReservationResultResponse(
                ReservationOutcome.RESERVED,
                activityId,
                userId,
                registrationId,
                null,
                null,
                occurredAt
        );
    }

    public static ReservationResultResponse waitlisted(
            Long activityId,
            Long userId,
            Long waitlistId,
            int position,
            LocalDateTime occurredAt) {
        return new ReservationResultResponse(
                ReservationOutcome.WAITLISTED,
                activityId,
                userId,
                null,
                waitlistId,
                position,
                occurredAt
        );
    }
}
