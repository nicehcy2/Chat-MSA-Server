package com.nicehcy2.entity;

import com.nicehcy2.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 프로필 변경 이벤트의 Outbox. 업무 트랜잭션과 같이 커밋되고 Debezium이 읽어 Kafka로 보낸다.
 * 컬럼명은 chat-service의 outbox와 같아 Debezium EventRouter 설정을 공유한다.
 */
@Getter
@Entity
@Table(name = "user_outbox", indexes = @Index(name = "idx_user_outbox_created_at", columnList = "created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    // 파티션 키. 같은 유저의 변경 순서가 유지된다
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "message_dto_payload", columnDefinition = "TEXT", nullable = false)
    private String messageDtoPayload;

    public UserOutbox(String aggregateType, String aggregateId, String eventType, String messageDtoPayload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.messageDtoPayload = messageDtoPayload;
    }
}
