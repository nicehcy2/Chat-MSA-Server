package com.nicehcy.chatservice.dto;

import lombok.Builder;

import com.nicehcy.chatservice.entity.enums.MessageType;
import java.time.LocalDateTime;

@Builder
public record MessageResponseDto(

        String messageTSID,
        String correlationId, // 전송 확인을 위해 클라이언트가 생성한 UUID
        Long chatRoomId, // 목적지(전달할 그룹 채팅방) ID
        Long senderId, // 발신인 ID
        MessageType messageType, // 메시지 타입(텍스트, 사진, 영수증)
        String content, // 메시지 내용
        LocalDateTime timestamp, // 타임스탬프
        Integer unreadCount // 읽지 않은 사용자
) {
}
