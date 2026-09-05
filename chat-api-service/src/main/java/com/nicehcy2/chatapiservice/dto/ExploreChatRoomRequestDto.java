package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder(toBuilder = true)
public record ExploreChatRoomRequestDto(
        @Size(max = 50, message = "검색어는 50자 이하여야 합니다.")
        String q,
        AgeGroup ageGroup,
        JobGroup jobGroup,
        Long before,
        Integer limit
) { }
