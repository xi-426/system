package com.yan.campuspass.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.campuspass.notification.config.RabbitNotificationConfig;
import com.yan.campuspass.notification.domain.MessageConsumeRecord;
import com.yan.campuspass.notification.domain.Notification;
import com.yan.campuspass.notification.event.NotificationEvent;
import com.yan.campuspass.notification.mapper.MessageConsumeRecordMapper;
import com.yan.campuspass.notification.mapper.NotificationMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Service
public class NotificationMessagingService {
    private final RabbitTemplate rabbitTemplate;
    private final NotificationMapper notificationMapper;
    private final MessageConsumeRecordMapper consumeRecordMapper;

    public NotificationMessagingService(
            RabbitTemplate rabbitTemplate,
            NotificationMapper notificationMapper,
            MessageConsumeRecordMapper consumeRecordMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.notificationMapper = notificationMapper;
        this.consumeRecordMapper = consumeRecordMapper;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void sendAfterCommit(NotificationEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitNotificationConfig.EXCHANGE,
                RabbitNotificationConfig.ROUTING_KEY,
                event
        );
    }

    @RabbitListener(queues = RabbitNotificationConfig.QUEUE)
    @Transactional
    public void consume(NotificationEvent event) {
        Long consumedCount = consumeRecordMapper.selectCount(
                new LambdaQueryWrapper<MessageConsumeRecord>()
                        .eq(MessageConsumeRecord::getEventId, event.eventId())
        );
        if (consumedCount > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setEventId(event.eventId());
        notification.setType(event.type());
        notification.setTitle(event.title());
        notification.setContent(event.content());
        notification.setReadFlag(false);
        notification.setCreatedAt(now);
        notificationMapper.insert(notification);

        MessageConsumeRecord consumeRecord = new MessageConsumeRecord();
        consumeRecord.setEventId(event.eventId());
        consumeRecord.setConsumedAt(now);
        consumeRecordMapper.insert(consumeRecord);
    }
}
