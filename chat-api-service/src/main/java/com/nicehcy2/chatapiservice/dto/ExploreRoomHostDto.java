package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.JobGroup;

public record ExploreRoomHostDto(
        Long userId,
        String nickname,
        String imageUrl,
        AgeGroup ageGroup,
        JobGroup jobGroup
) { }
