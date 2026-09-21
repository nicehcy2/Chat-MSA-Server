package com.nicehcy.chatservice.dto;

import java.time.LocalDateTime;

/** chat-api-service의 MembershipEvent와 필드명 1:1 */
public record MembershipEventDto(
        String eventId,
        MembershipEventType type,
        Long chatRoomId,
        Long userId,
        LocalDateTime occurredAt
) {

    public enum MembershipEventType {
        JOINED, LEFT, KICKED
    }
}
