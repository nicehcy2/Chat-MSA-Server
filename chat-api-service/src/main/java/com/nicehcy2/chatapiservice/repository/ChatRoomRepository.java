package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
