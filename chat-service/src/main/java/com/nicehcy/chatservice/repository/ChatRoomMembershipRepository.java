package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.ChatRoom;
import com.nicehcy.chatservice.entity.ChatRoomMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMembershipRepository extends JpaRepository<ChatRoomMembership, Long> {

    Optional<ChatRoomMembership> findByUserIdAndChatRoom(Long userId, ChatRoom chatRoom);
    List<ChatRoomMembership> findByChatRoomId(Long chatRoomId);
}
