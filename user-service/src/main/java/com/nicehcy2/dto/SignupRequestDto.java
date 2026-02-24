package com.nicehcy2.dto;

import com.nicehcy2.entity.AgeGroup;
import com.nicehcy2.entity.JobGroup;
import com.nicehcy2.entity.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SignupRequestDto(
        String nickname,
        String gender,
        String email,
        String password,
        String birthDay,
        String imageUrl,
        UserRole userRole,
        AgeGroup ageGroup,
        JobGroup jobGroup,
        Integer reward,
        Boolean status
) {
}
