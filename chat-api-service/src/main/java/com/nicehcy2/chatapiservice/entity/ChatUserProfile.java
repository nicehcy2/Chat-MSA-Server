package com.nicehcy2.chatapiservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * user-service 프로필의 채팅용 사본. 갱신은 chat-service의 이벤트 컨슈머만 한다.
 * users 테이블 대신 이 테이블만 조인해 user-service 스키마와의 결합을 끊는다.
 */
@Getter
@Entity
@Immutable
@Builder
@Table(name = "chat_user_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatUserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_group", nullable = false)
    private JobGroup jobGroup;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
