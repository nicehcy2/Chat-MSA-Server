package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.ChatRoomMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRoomMembershipRepository extends JpaRepository<ChatRoomMembership, Long> {

    @Query("SELECT cm.chatRoom FROM ChatRoomMembership cm WHERE cm.userId = :userId")
    List<ChatRoom> findChatRoomByUserId(Long userId);
}
