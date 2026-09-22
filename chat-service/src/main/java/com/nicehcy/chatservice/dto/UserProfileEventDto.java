package com.nicehcy.chatservice.dto;

/**
 * user-service의 UserProfileChangedEvent와 필드명 1:1.
 * enum은 문자열로 받는다. user-service가 값을 추가해도 컨슈머가 죽지 않고 UNDECIDED로 흡수한다.
 */
public record UserProfileEventDto(
        Long userId,
        String nickname,
        String imageUrl,
        String ageGroup,
        String jobGroup,
        boolean active
) {
}
