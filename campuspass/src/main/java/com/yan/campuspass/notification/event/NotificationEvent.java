package com.yan.campuspass.notification.event;

public record NotificationEvent(
        String eventId,
        Long userId,
        String type,
        String title,
        String content
) {
}
