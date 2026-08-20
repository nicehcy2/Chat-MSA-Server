package com.nicehcy.chatservice.entity;

import com.nicehcy.chatservice.entity.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
// 방별 메시지 조회는 항상 id(=시간순) 정렬로 나가므로, 복합 인덱스로 정렬 비용을 없앤다.
@Table(indexes = @Index(name = "idx_message_room_id", columnList = "chat_room_id, id"))
public class Message {

    @Id
    private Long id;

    @Column(name = "chat_room_id")
    private Long chatRoomId; // 목적지(전달할 그룹 채팅방) ID

    @Column(name = "sender_id")
    private Long senderId; // 발신인 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType; // 메시지 타입(텍스트, 사진, 영수증)

    @Column(name = "content")
    private String content; // 메시지 내용

    @Column(name = "timestamp")
    private LocalDateTime timestamp; // 타임스탬프
}
