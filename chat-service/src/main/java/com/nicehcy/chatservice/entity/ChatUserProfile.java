package com.nicehcy.chatservice.entity;

import com.nicehcy.chatservice.entity.enums.AgeGroup;
import com.nicehcy.chatservice.entity.enums.JobGroup;
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

import java.time.LocalDateTime;

/**
 * user-service 프로필의 채팅용 사본. 이 서비스의 프로필 이벤트 컨슈머가 유일한 갱신 주체다.
 */
@Getter
@Entity
@Builder
@Table(name = "chat_user_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    public void apply(String nickname, String imageUrl, AgeGroup ageGroup, JobGroup jobGroup, boolean active) {
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.ageGroup = ageGroup;
        this.jobGroup = jobGroup;
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }
}
