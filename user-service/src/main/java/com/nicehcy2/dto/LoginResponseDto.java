package com.nicehcy2.dto;

import lombok.Builder;

@Builder
public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        String sessionId, // familyId
        Long userId
) {
}
