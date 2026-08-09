package com.yan.campuspass.notification.service;

import com.yan.campuspass.notification.config.RabbitNotificationConfig;
import com.yan.campuspass.notification.domain.MessageConsumeRecord;
import com.yan.campuspass.notification.domain.Notification;
import com.yan.campuspass.notification.event.NotificationEvent;
import com.yan.campuspass.notification.mapper.MessageConsumeRecordMapper;
import com.yan.campuspass.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationMessagingServiceTest {

    private RabbitTemplate rabbitTemplate;
    private NotificationMapper notificationMapper;
    private MessageConsumeRecordMapper consumeRecordMapper;
    private NotificationMessagingService messagingService;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        notificationMapper = mock(NotificationMapper.class);
        consumeRecordMapper = mock(MessageConsumeRecordMapper.class);
        messagingService = new NotificationMessagingService(
                rabbitTemplate,
                notificationMapper,
                consumeRecordMapper
        );
    }

    @Test
    void shouldSendEventToNotificationExchange() {
        NotificationEvent event = event();

        messagingService.sendAfterCommit(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitNotificationConfig.EXCHANGE,
                RabbitNotificationConfig.ROUTING_KEY,
                event
        );
    }

    @Test
    void shouldPersistNotificationAndConsumeRecord() {
        when(consumeRecordMapper.selectCount(any())).thenReturn(0L);

        messagingService.consume(event());

        verify(notificationMapper).insert(any(Notification.class));
        verify(consumeRecordMapper)
                .insert(any(MessageConsumeRecord.class));
    }

    @Test
    void shouldIgnoreAlreadyConsumedEvent() {
        when(consumeRecordMapper.selectCount(any())).thenReturn(1L);

        messagingService.consume(event());

        verify(notificationMapper, never())
                .insert(any(Notification.class));
        verify(consumeRecordMapper, never())
                .insert(any(MessageConsumeRecord.class));
    }

    private NotificationEvent event() {
        return new NotificationEvent(
                "event-1",
                2L,
                "RESERVED",
                "预约成功",
                "你已成功预约活动 10"
        );
    }
}
