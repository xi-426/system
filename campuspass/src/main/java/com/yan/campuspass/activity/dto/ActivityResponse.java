package com.yan.campuspass.activity.dto;

import com.yan.campuspass.activity.domain.Activity;
import com.yan.campuspass.activity.domain.ActivityStatus;

import java.time.LocalDateTime;

public record ActivityResponse(
        Long id,
        Long organizerId,
        String title,
        String category,
        String location,
        String description,
        Integer capacity,
        Integer registeredCount,
        LocalDateTime registrationStartTime,
        LocalDateTime registrationEndTime,
        LocalDateTime activityStartTime,
        LocalDateTime activityEndTime,
        ActivityStatus status,
        LocalDateTime createdAt
) {
    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getOrganizerId(),
                activity.getTitle(),
                activity.getCategory(),
                activity.getLocation(),
                activity.getDescription(),
                activity.getCapacity(),
                activity.getRegisteredCount(),
                activity.getRegistrationStartTime(),
                activity.getRegistrationEndTime(),
                activity.getActivityStartTime(),
                activity.getActivityEndTime(),
                activity.getStatus(),
                activity.getCreatedAt()
        );
    }
}
