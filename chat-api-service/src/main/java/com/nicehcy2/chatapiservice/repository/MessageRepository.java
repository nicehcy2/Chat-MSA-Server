package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.dto.ChatRoomLastMessageDto;
import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
        select new com.nicehcy2.chatapiservice.dto.MessageDto(
            m.id,
            m.chatRoomId,
            m.senderId,
            m.messageType,
            m.content,
            m.timestamp,
            u.imageUrl,
            u.nickname
        )
        from Message m
        join User u on m.senderId = u.userId
        where m.chatRoomId = :chatRoomId
        order by m.id asc
    """)
    List<MessageDto> findCustomByChatRoomId(Long chatRoomId);

    /**
     * 커서 기반 메시지 조회 (최신 → 과거 방향).
     *
     * - before: 이 ID 미만(exclusive)만 조회. null이면 방의 최신 메시지부터.
     * - floor: 멤버십의 joinMessageId. 입장 이전 히스토리 차단
     * - 정렬은 DESC(커서 진행 방향). 클라이언트에 줄 때는 서비스에서 ASC로 뒤집는다.
     * - (chat_room_id, id) 복합 인덱스(idx_message_room_id)를 그대로 탄다.
     */
    @Query("""
        select new com.nicehcy2.chatapiservice.dto.MessageDto(
            m.id,
            m.chatRoomId,
            m.senderId,
            m.messageType,
            m.content,
            m.timestamp,
            u.imageUrl,
            u.nickname
        )
        from Message m
        join User u on m.senderId = u.userId
        where m.chatRoomId = :chatRoomId
          and (:before is null or m.id < :before)
          and (:floor is null or m.id > :floor)
        order by m.id desc
    """)
    List<MessageDto> findMessagesBefore(Long chatRoomId, Long before, Long floor, Pageable pageable);

    @Query("""
        select new com.nicehcy2.chatapiservice.dto.ChatRoomLastMessageDto(
            m.chatRoomId, m.id, m.messageType, m.content, m.timestamp)
        from Message m
        where m.id in (
            select max(m2.id) from Message m2
            where m2.chatRoomId in :chatRoomIds
            group by m2.chatRoomId
        )
    """)
    List<ChatRoomLastMessageDto> findLastMessages(List<Long> chatRoomIds);
}
