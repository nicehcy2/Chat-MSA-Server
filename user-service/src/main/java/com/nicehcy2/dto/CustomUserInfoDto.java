package com.nicehcy2.dto;

import com.nicehcy2.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

@Builder
public record CustomUserInfoDto(
        Long userId,
        String nickname,
        String email,
        String password,
        UserRole role) {
}
