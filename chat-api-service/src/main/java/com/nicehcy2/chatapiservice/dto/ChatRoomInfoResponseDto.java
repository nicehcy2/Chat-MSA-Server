package com.nicehcy2.chatapiservice.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatRoomInfoResponseDto(
        Long chatRoomId,
        String chatRoomTitle,
        Integer chatRoomMaxUserCount,
        String chatRoomRule,
        String chatRoomThumbnail,
        Integer participationCount, // 현재 인원수
        String lastChatMessage,
        Integer unreadChatCount,
        LocalDateTime updatedAt
        )
{ }
