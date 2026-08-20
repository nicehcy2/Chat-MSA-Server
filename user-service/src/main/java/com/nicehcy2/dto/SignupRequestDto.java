package com.nicehcy2.dto;

import com.nicehcy2.entity.enums.AgeGroup;
import com.nicehcy2.entity.enums.JobGroup;
import lombok.Builder;

// userRole, reward, status는 서버가 결정하므로 요청으로 받지 않는다. (권한 상승 방지)
@Builder
public record SignupRequestDto(
        String nickname,
        String gender,
        String email,
        String password,
        String birthDay,
        String imageUrl,
        AgeGroup ageGroup,
        JobGroup jobGroup
) {
}
