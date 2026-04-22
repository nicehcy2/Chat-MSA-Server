package com.nicehcy2.pushnotificationservice.messaging.consumer;

import com.nicehcy2.pushnotificationservice.dto.PushNotificationRequestDto;
import com.nicehcy2.pushnotificationservice.entity.FcmToken;
import com.nicehcy2.pushnotificationservice.repository.FcmTokenRepository;
import com.nicehcy2.pushnotificationservice.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationConsumer {

    private final FcmTokenRepository fcmTokenRepository;
    private final PushNotificationService pushNotificationService;

    @KafkaListener(topics = "${PUSH_NOTIFICATION_TOPIC:push-notification-topic}")
    public void listenKafkaPushNotificationRecord(@Payload final PushNotificationRequestDto pushNotificationRequestDto) {

        log.info("푸시 알림 Kafka 리스너 메시지 수신 [{}]", pushNotificationRequestDto.messageDto().id());

        // 처음에는 Redis에서 조회
        // Redis에 없으면 DB 조회
        List<String> fcmTokens = fcmTokenRepository.findByUserIdIn(pushNotificationRequestDto.targetOfflineUserIds())
                .stream()
                .map(FcmToken::getToken)
                .toList();

        pushNotificationService.sendPushToOfflineUsers(pushNotificationRequestDto.messageDto(), fcmTokens);
    }
}
