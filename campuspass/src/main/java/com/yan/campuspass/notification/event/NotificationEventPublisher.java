package com.yan.campuspass.notification.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEventPublisher {
    private final ApplicationEventPublisher publisher;
    public NotificationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Long userId, String type, String title, String content) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(),
                userId,
                type,
                title,
                content
        );
        publisher.publishEvent(event);
    }
}
