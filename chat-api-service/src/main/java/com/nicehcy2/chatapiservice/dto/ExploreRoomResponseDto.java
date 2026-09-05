package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record ExploreRoomResponseDto(
        Long chatRoomId,
        String title,
        String description,
        Integer participationCount,
        Integer maxParticipants,
        Integer dailyLimit,
        Boolean isPrivate,
        String imageUrl,
        Set<AgeGroup> ageGroups,
        Set<JobGroup> jobGroups,
        Boolean isBanned,
        LocalDateTime createdAt,
        ExploreRoomHostDto host
) { }
