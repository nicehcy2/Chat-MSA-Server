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

    // 활성 멤버 정의는 chat-service의 구독·전송 가드, 워터마크 UPDATE 조건과 같아야 한다
    public boolean isActive() {
        return leftAt == null && !isBanned;
    }

    // 나가 있던 동안의 대화가 보이지 않도록 floor를 새로 잡는다. isBanned는 건드리지 않는다
    public void rejoin(Long joinMessageId) {
        this.leftAt = null;
        this.joinMessageId = joinMessageId;
        this.joinedAt = LocalDateTime.now();
    }

    // rejoin이 isHost를 건드리지 않으므로 여기서 내려야 재참여 시 호스트가 둘이 되지 않는다.
    // joinMessageId와 lastReadMessageId는 재참여 시 워터마크로 쓰이므로 유지
    public void leave() {
        this.leftAt = LocalDateTime.now();
        this.isHost = false;
    }

    public void promoteToHost() {
        this.isHost = true;
    }

    public void ban() {
        LocalDateTime now = LocalDateTime.now();
        this.isBanned = true;
        this.bannedAt = now;
        this.leftAt = now;
        this.isHost = false;
    }
}