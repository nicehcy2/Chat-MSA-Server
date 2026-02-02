package com.nicehcy2.dto;

import lombok.Builder;

@Builder
public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        String sessionId,
        Long userId
        //TODO: 검토 후 SessionID나 FamilyID 추가
) {
}
