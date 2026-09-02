package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.dto.ChatRoomParticipantDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomUnreadCountDto;
import com.nicehcy2.chatapiservice.entity.ChatRoomMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMembershipRepository extends JpaRepository<ChatRoomMembership, Long> {

    @Query("""
            SELECT cm FROM ChatRoomMembership cm
            JOIN FETCH cm.chatRoom
            WHERE cm.userId = :userId
              AND cm.leftAt IS NULL AND cm.isBanned = false
            """)
    List<ChatRoomMembership> findActiveMembershipsWithChatRoom(Long userId);

    // 안 읽은 수 = 워터마크(없으면 입장 시점, 그것도 없으면 0) 이후 메시지 수
    @Query("""
            SELECT new com.nicehcy2.chatapiservice.dto.ChatRoomUnreadCountDto(m.chatRoomId, COUNT(m))
            FROM Message m
            JOIN ChatRoomMembership cm ON cm.chatRoom.id = m.chatRoomId
            WHERE cm.userId = :userId
              AND cm.leftAt IS NULL AND cm.isBanned = false
              AND m.id > COALESCE(cm.lastReadMessageId, cm.joinMessageId, 0)
            GROUP BY m.chatRoomId
            """)
    List<ChatRoomUnreadCountDto> countUnreadByUserId(Long userId);

    // 활성 멤버십 조회 — 인가 판정과 joinMessageId(히스토리 floor) 획득을 쿼리 1번으로
    Optional<ChatRoomMembership> findByChatRoomIdAndUserIdAndLeftAtIsNullAndIsBannedFalse(Long chatRoomId, Long userId);

    // 필터 조건은 chat-service의 워터마크 UPDATE 가드(leftAt IS NULL AND isBanned = false)와 동일하게 유지한다.
    @Query("""
            SELECT new com.nicehcy2.chatapiservice.dto.ChatRoomParticipantDto(
                u.userId, u.nickname, u.imageUrl, cm.isHost, cm.lastReadMessageId)
            FROM ChatRoomMembership cm
            JOIN User u ON cm.userId = u.userId
            WHERE cm.chatRoom.id = :chatRoomId
              AND cm.leftAt IS NULL AND cm.isBanned = false
            """)
    List<ChatRoomParticipantDto> findParticipantsByChatRoomId(Long chatRoomId);
}
