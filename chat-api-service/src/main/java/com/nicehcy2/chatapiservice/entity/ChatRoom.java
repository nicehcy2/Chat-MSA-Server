package com.nicehcy2.chatapiservice.entity;

import com.nicehcy2.chatapiservice.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 18)
    private String title;

    @Column(name = "password", length = 4)
    private String password;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "participation_count", nullable = false)
    private Integer participationCount;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;

    // 빈 Set = 전체 대상. 목록 조회 시 방마다 컬렉션 쿼리가 나가지 않도록 BatchSize로 묶는다
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "chat_room_age_group", joinColumns = @JoinColumn(name = "chat_room_id"))
    @Column(name = "age_group", nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<AgeGroup> ageGroups = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "chat_room_job_group", joinColumns = @JoinColumn(name = "chat_room_id"))
    @Column(name = "job_group", nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<JobGroup> jobGroups = new HashSet<>();
}
