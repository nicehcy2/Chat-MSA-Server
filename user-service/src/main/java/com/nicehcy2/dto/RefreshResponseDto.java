package com.nicehcy2.dto;

import lombok.Builder;

@Builder
public record RefreshResponseDto(
        String accessToken,
        String sessionId,
        Long userId
) {
}
