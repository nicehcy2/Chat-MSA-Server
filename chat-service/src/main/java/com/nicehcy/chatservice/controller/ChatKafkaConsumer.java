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

    // 다중 채팅 서버 적용 시 각 채팅 서버마다 groupId를 다르게 설정해야 한다.
    @KafkaListener(topics = "chat-topic", groupId = "${CHAT_NODE_ID}")
    public void listenKafkaChatMessage(@Payload final MessageDto messageDto) {

        log.info("[5/6] Kafka 리스너 수신 메시지 전체 내용: {}", messageDto);

        // if (해당 서버에 전송할 채팅방의 구독자가 한 명 이상 있다면) -> 전송

        final String destination = "/sub/chatroom" + messageDto.chatRoomId();
        messagingTemplate.convertAndSend(destination, messageDto);
        log.info("[6/6] STOMP over WebSocket을 통해 메시지 전송");
    }
}
