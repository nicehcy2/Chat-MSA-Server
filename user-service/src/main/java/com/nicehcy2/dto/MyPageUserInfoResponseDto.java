package com.nicehcy2.dto;

import com.nicehcy2.entity.enums.UserRole;
import lombok.Builder;

@Builder
public record MyPageUserInfoResponseDto(
        Long userId,
        String nickname,
        UserRole userRole,
        String ageGroup,
        String jobGroup,
        String email,
        int reward,
        int dayTargetExpenditure
) {
}
