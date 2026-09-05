package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
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
        MembershipStatus membershipStatus,
        LocalDateTime createdAt,
        ExploreRoomHostDto host
) {

    public static ExploreRoomResponseDto from(ChatRoom room, ExploreRoomHostDto host, MembershipStatus membershipStatus) {
        return ExploreRoomResponseDto.builder()
                .chatRoomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription())
                .participationCount(room.getParticipationCount())
                .maxParticipants(room.getMaxParticipants())
                .dailyLimit(room.getDailyLimit())
                .isPrivate(room.getPassword() != null)
                .imageUrl(room.getImageUrl())
                .ageGroups(room.getAgeGroups())
                .jobGroups(room.getJobGroups())
                .membershipStatus(membershipStatus)
                .createdAt(room.getCreatedAt())
                .host(host)
                .build();
    }
}
