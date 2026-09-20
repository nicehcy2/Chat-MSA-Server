package com.nicehcy2.chatapiservice.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 멤버십 변화 이벤트. 필드명은 chat-service의 MembershipEventDto와 1:1.
 * eventId는 컨슈머의 멱등 키, chatRoomId는 파티션 키다.
 */
public record MembershipEvent(
        String eventId,
        MembershipEventType type,
        Long chatRoomId,
        Long userId,
        LocalDateTime occurredAt
) {

    public static MembershipEvent joined(Long chatRoomId, Long userId) {
        return of(MembershipEventType.JOINED, chatRoomId, userId);
    }

    public static MembershipEvent left(Long chatRoomId, Long userId) {
        return of(MembershipEventType.LEFT, chatRoomId, userId);
    }

    public static MembershipEvent kicked(Long chatRoomId, Long userId) {
        return of(MembershipEventType.KICKED, chatRoomId, userId);
    }

    private static MembershipEvent of(MembershipEventType type, Long chatRoomId, Long userId) {
        return new MembershipEvent(UUID.randomUUID().toString(), type, chatRoomId, userId, LocalDateTime.now());
    }
}
