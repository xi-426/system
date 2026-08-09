package com.yan.campuspass.dashboard.dto;

import com.yan.campuspass.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String content,
        Boolean read,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReadFlag(),
                notification.getCreatedAt()
        );
    }
}
