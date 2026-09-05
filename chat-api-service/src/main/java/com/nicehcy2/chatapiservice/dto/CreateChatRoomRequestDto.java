package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * 단일 필드 규칙만 어노테이션으로 검증한다. 길이는 trim 전 raw 문자열 기준.
 * "비공개면 비밀번호 필수" 같은 필드 간 규칙은 ChatRoomService가 판정한다.
 */
@Builder(toBuilder = true)
public record CreateChatRoomRequestDto(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 18, message = "제목은 1~18자여야 합니다.")
        String title,

        @Size(max = 200, message = "설명은 200자 이하여야 합니다.")
        String description,

        @NotNull(message = "최대 인원은 필수입니다.")
        @Min(value = 1, message = "최대 인원은 1~100명이어야 합니다.")
        @Max(value = 100, message = "최대 인원은 1~100명이어야 합니다.")
        Integer maxParticipants,

        @NotNull(message = "공개 여부는 필수입니다.")
        Boolean isPrivate,

        @Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
        String password,

        List<AgeGroup> ageGroups,

        List<JobGroup> jobGroups,

        @NotNull(message = "일일 한도는 필수입니다.")
        @Min(value = 0, message = "일일 한도는 0 이상이어야 합니다.")
        Integer dailyLimit,

        @Size(max = 255, message = "이미지 URL은 255자 이하여야 합니다.")
        String imageUrl
) { }
