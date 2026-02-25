package com.nicehcy2.dto;

import lombok.Builder;

@Builder
public record UserInfoRequestDto(
        String nickname,
        String gender,
        String ageGroup,
        String jobGroup,
        String imageUrl
) {
}
