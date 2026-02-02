package com.nicehcy2.dto;

public record RefreshResponseDto(
        String accessToken
        //TODO: 검토 후 SessionID나 FamilyID 추가
) {
}
