package com.nicehcy2.dto;

import com.nicehcy2.entity.UserRole;
import lombok.Builder;

@Builder
public record CustomUserInfoDto(
        Long userId,
        String email,
        UserRole role,
        String sessionId) {
}
