package com.nicehcy.chatservice.messaging.producer;

import com.nicehcy.chatservice.dto.PushNotificationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, PushNotificationRequestDto> kafkaTemplate;
    @Value("${PUSH_NOTIFICATION_TOPIC:push-notification-topic}")
    private String PUSH_NOTIFICATION_TOPIC;

    public void producePushNotification(final PushNotificationRequestDto pushNotificationRequestDto) {

        final String chatRoomId = pushNotificationRequestDto.messageDto().chatRoomId().toString();
        kafkaTemplate.send(PUSH_NOTIFICATION_TOPIC, chatRoomId, pushNotificationRequestDto)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("푸시 알림 Kafka 전송 실패 [{}]", pushNotificationRequestDto.messageDto().id(), ex);
                    } else {
                        log.info("푸시 알림 Kafka 전송 성공 [{}]", pushNotificationRequestDto.messageDto().id());
                    }
                });
    }
}
