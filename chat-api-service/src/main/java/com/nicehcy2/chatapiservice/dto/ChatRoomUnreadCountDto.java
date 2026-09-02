package com.nicehcy2.chatapiservice.dto;

public record ChatRoomUnreadCountDto(
        Long chatRoomId,
        Long count
) {
}
