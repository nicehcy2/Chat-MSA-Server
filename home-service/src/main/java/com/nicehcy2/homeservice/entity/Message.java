package com.nicehcy2.homeservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    @Column(name = "chat_room_id")
    private Long chatRoomId; // 목적지(전달할 그룹 채팅방) ID

    @Column(name = "sender_id")
    private Long senderId; // 발신인 ID

    @Column(name = "message_type")
    private String messageType; // 메시지 타입(텍스트, 사진, 영수증)

    @Column(name = "content")
    private String content; // 메시지 내용

    @Column(name = "timestamp")
    private String timestamp; // 타임스탬프

    @Column(name = "unread_count")
    private Integer unreadCount; // 읽지 않은 사용자
}
