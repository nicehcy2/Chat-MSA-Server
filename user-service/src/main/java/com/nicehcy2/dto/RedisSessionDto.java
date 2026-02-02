package com.nicehcy2.dto;

import lombok.Builder;

@Builder
public record RedisSessionDto(
        CustomUserInfoDto customUserInfoDto,
        String rtHash,
        long expiresAtEpoch
) {
}
