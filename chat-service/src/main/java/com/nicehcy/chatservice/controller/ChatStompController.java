package com.nicehcy.chatservice.controller;

import com.nicehcy.chatservice.dto.MessageSendRequestDto;
import com.nicehcy.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

    /**
     * 채팅 메시지를 특정 채팅방(roomId)으로 전송.
     * 클라이언트가 STOMP 프로토콜을 사용해 "chat.message.{roomId}" 경로로 메시지를 전송하면 해당 메서드가 메시지를 처리(서비스 계층으로 위임).
     * 클라이언트가 WebSocket을 보낸 메시지를 처리할 서버 측 핸들러 메서드를 지정
     *
     * @param roomId                채팅방 ID (STOMP 경로 변수)
     * @param messageSendRequestDto 전송된 메시지 데이터
     * @param headerAccessor        STOMP 세션 정보 (CONNECT 시 JWT에서 추출한 userId 보관)
     */
    @MessageMapping("chat.message.{roomId}")
    public void publishMessage(
            @DestinationVariable String roomId,
            MessageSendRequestDto messageSendRequestDto,
            SimpMessageHeaderAccessor headerAccessor) {

        // 발신자 ID는 클라이언트 페이로드가 아니라 CONNECT 시점에 JWT에서 추출해 세션에 저장한 값을 사용한다.
        Long senderId = Long.parseLong((String) headerAccessor.getSessionAttributes().get("userId"));
        chatService.sendMessage(messageSendRequestDto, senderId);
    }

    @GetMapping("/test")
    public String test() {
        log.info("test() has been called");
        return "Test!";
    }
}
