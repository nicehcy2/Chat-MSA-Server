package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.enums.MessageType;

import java.time.LocalDateTime;

public record ChatRoomLastMessageDto(
        Long chatRoomId,
        Long messageId,
        MessageType messageType,
        String content,
        LocalDateTime timestamp
) {
}
