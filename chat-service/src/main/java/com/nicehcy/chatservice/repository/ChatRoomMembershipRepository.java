package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.ChatRoom;
import com.nicehcy.chatservice.entity.ChatRoomMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMembershipRepository extends JpaRepository<ChatRoomMembership, Long> {

    Optional<ChatRoomMembership> findByUserIdAndChatRoom(Long userId, ChatRoom chatRoom);
    List<ChatRoomMembership> findByChatRoomId(Long chatRoomId);

    @Transactional
    @Modifying
    @Query("""
        update ChatRoomMembership m
           set m.lastReadMessageId = :lastReadMessageId
         where m.userId = :userId
           and m.chatRoom.id = :chatRoomId
           and m.leftAt is null
           and m.isBanned = false
           and (m.lastReadMessageId is null or m.lastReadMessageId < :lastReadMessageId)
    """)
    int updateLastReadMessageId(Long chatRoomId, Long userId, Long lastReadMessageId);
}
