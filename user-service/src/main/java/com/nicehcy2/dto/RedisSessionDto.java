package com.nicehcy2.dto;

import lombok.Builder;

@Builder
public record RedisSessionDto(
        CustomUserInfoDto customUserInfoDto,
        String rtHash, // 현재 RT 해시
        String prevRtHash, // 이전 RT 해시
        String currentAccessJti, // 현재 유효한 AccessToken의 jti
        Long rotatedAtEpoch, // 새로 교체된 시각
        long expiresAtEpoch // 절대 만료 시각 (2단계에서 검증 추가 예정)
) {
}
