package com.nicehcy.chatservice.messaging.consumer;

import com.nicehcy.chatservice.dto.MessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${CHAT_NODE_ID}") private String chatNodeId;
    @Value("${IDEMPOTENCY_TTL_DAYS:1}") private long idempotencyTtlDays;

    // 다중 채팅 서버 적용 시 각 채팅 서버마다 groupId를 다르게 설정해야 한다.
    @KafkaListener(topics = "${CHAT_TOPIC:chat-topic}", groupId = "${CHAT_NODE_ID}")
    public void listenKafkaChatMessage(@Payload final MessageResponseDto messageDto) {

        log.info("[5/6] Kafka 리스너 메시지 수신 [{}]", messageDto.messageTSID());

        if (!tryMarkAsProcessed(messageDto.messageTSID())) {
            log.debug("중복 메시지 스킵: {}", messageDto.messageTSID());
            return;
        }

        final String destination = "/sub/chatroom" + messageDto.chatRoomId();
        messagingTemplate.convertAndSend(destination, messageDto);
        log.info("[6/6] STOMP over WebSocket을 통해 메시지 전송");
    }

    // Redis SET NX: 키가 없으면 등록(true) + 처리 진행, 이미 있으면(false) 중복으로 스킵
    private boolean tryMarkAsProcessed(String messageId) {
        try {
            String key = "processed:ws:" + chatNodeId + ":" + messageId;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofDays(idempotencyTtlDays));
            return Boolean.TRUE.equals(isNew);
        } catch (Exception e) {
            log.warn("Redis 멱등성 체크 실패 - 중복 허용하고 처리 진행: {}", e.getMessage());
            return true;
        }
    }
}
