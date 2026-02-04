package com.nicehcy2.chatapiservice.dto;

public record ChatRoomInfoResponseDto(
        String chatRoomTitle,
        Integer chatRoomPassword,
        Integer chatRoomMaxUserCount,
        String chatRoomRule

) {
}
