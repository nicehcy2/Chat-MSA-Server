package com.nicehcy2.chatapiservice.entity;

import com.nicehcy2.chatapiservice.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // 재참여 시 직접 갱신하므로 @CreatedDate를 쓰지 않는다. 신규 생성 시에도 서비스가 세팅
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

    // 나가 있던 동안의 대화가 보이지 않도록 floor를 새로 잡는다. isBanned는 건드리지 않는다
    public void rejoin(Long joinMessageId) {
        this.leftAt = null;
        this.joinMessageId = joinMessageId;
        this.joinedAt = LocalDateTime.now();
    }
}