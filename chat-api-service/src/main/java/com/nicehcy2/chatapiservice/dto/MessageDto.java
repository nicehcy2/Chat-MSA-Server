package com.nicehcy2.chatapiservice.dto;

import lombok.Builder;

@Builder
public record MessageDto(
        String id,
        Long chatRoomId, // 목적지(전달할 그룹 채팅방) ID
        Long senderId, // 발신인 ID
        String messageType, // 메시지 타입(텍스트, 사진, 영수증)
        String content, // 메시지 내용
        String timestamp, // 타임스탬프
        Integer unreadCount, // 읽지 않은 사용자
        String senderImageUrl,
        String nickname
) {
}
