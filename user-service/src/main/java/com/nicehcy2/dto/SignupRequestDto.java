package com.nicehcy2.dto;

import com.nicehcy2.entity.enums.AgeGroup;
import com.nicehcy2.entity.enums.JobGroup;
import com.nicehcy2.entity.enums.UserRole;
import lombok.Builder;

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
