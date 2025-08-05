package com.nicehcy.chatservice.controller;

import com.nicehcy.chatservice.dto.MessageDto;
import com.nicehcy.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 채팅 메시지를 특정 채팅방(roomId)으로 전송.
     * 클라이언트가 STOMP 프로토콜을 사용해 "chat.message.{roomId}" 경로로 메시지를 전송하면 해당 메서드가 메시지를 처리(서비스 계층으로 위임).
     * 클라이언트가 WebSocket을 보낸 메시지를 처리할 서버 측 핸들러 메서드를 지정
     *
     * @param roomId      채팅방 ID (STOMP 경로 변수)
     * @param messageDto  전송된 메시지 데이터
     */
    @MessageMapping("chat.message.{roomId}")
    public void publishMessage(
            @DestinationVariable String roomId,
            @RequestBody MessageDto messageDto){

        chatService.sendMessage(messageDto); // 메시지 전송
    }
}
