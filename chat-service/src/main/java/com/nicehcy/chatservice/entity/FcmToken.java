package com.nicehcy.chatservice.entity;

import com.nicehcy.chatservice.common.BaseEntity;
import com.nicehcy.chatservice.entity.enums.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 푸시 토큰. 등록·삭제는 chat-api-service, 조회는 이 서비스의 푸시 컨슈머.
 * userId는 유저 컨텍스트 밖 참조라 FK를 두지 않는다.
 */
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "fcm_token")
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;
}
