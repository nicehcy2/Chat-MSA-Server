package com.nicehcy2.dto;

import lombok.Builder;

@Builder
public record AccessTokenResponseDto(
        String accessToken,
        Long userId
) {
}
