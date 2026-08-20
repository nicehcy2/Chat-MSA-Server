package com.nicehcy.chatservice.entity;

import com.nicehcy.chatservice.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 워터마크 갱신(user_id + chatroom_id 조건 UPDATE)이 메시지 전송/읽음 처리마다 실행되므로,
// FK 인덱스로 방 멤버 전체를 훑지 않고 행을 바로 찾도록 복합 인덱스를 둔다.
@Table(indexes = @Index(name = "idx_membership_user_room", columnList = "user_id, chatroom_id"))
public class ChatRoomMembership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id")
    private ChatRoom chatRoom;

    @Column(name = "is_host", nullable = false)
    private Boolean isHost;

    @CreatedDate
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "join_message_id")
    private Long joinMessageId;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}