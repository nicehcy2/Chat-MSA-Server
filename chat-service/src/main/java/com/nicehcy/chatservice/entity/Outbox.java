package com.nicehcy.chatservice.entity;

import com.nicehcy.chatservice.common.BaseEntity;
import com.nicehcy.chatservice.entity.enums.OutboxStatus;
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
public class Outbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_dto_payload", columnDefinition = "TEXT", nullable = false)
    private String messageDtoPayload; // MessageDto JSON 직렬화

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType; // 어떤 도메인 이벤트인지
    private String aggregateId;  // 이벤트 ID

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // 이벤트 종류

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    private int retryCount;
    private LocalDateTime publishedAt;
    private LocalDateTime failedAt;

    public Outbox(String aggregateType, String aggregateId, String eventType, String messageDtoPayload) {

        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.messageDtoPayload = messageDtoPayload;
        this.retryCount = 0;
        this.status = OutboxStatus.PENDING;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.retryCount++;
    }

    public void resetToPending() {
        this.status = OutboxStatus.PENDING;
    }
}
