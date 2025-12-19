package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    @Query("""
        select new com.nicehcy2.chatapiservice.dto.MessageDto(
            m.id,
            m.chatRoomId,
            m.senderId,
            m.messageType,
            m.content,
            m.timestamp,
            m.unreadCount,
            u.imageUrl,
            u.nickname
        )
        from Message m
        join User u on m.senderId = u.userId
        where m.chatRoomId = :chatRoomId
        order by m.timestamp asc
    """)
    List<MessageDto> findCustomByChatRoomId(Long chatRoomId);
}
