package com.nicehcy2.dto;

import com.nicehcy2.entity.UserRole;

public record SignupRequestDto(
        String nickname,
        String gender,
        String email,
        String password,
        String imageUrl,
        UserRole userRole
) {
}
