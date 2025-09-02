package com.nicehcy.chatservice.controller;

import com.nicehcy.chatservice.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "chat-topic", groupId = "chat-messages-group")
    public void listenKafkaChatMessage(@Payload final MessageDto messageDto) {

        log.info("[5/6] Kafka 리스너 수신 메시지 전체 내용: {}", messageDto);

        final String destination = "/sub/chatroom" + messageDto.chatRoomId();
        messagingTemplate.convertAndSend(destination, messageDto);
        log.info("[6/6] STOMP over WebSocket을 통해 메시지 전송");
    }
}
