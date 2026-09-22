package com.nicehcy2.chatapiservice.entity;

import com.nicehcy2.chatapiservice.common.BaseEntity;
import com.nicehcy2.chatapiservice.entity.enums.DeviceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 푸시 토큰. 알림을 보내는 채팅 도메인이 소유한다.
 * userId는 유저 컨텍스트 밖 참조라 FK를 두지 않는다. 탈퇴 시 삭제는 프로필 이벤트 컨슈머가 맡는다.
 */
@Getter
@Entity
@Builder
@Table(name = "fcm_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 기기+앱 설치당 하나. 중복 행이 쌓이면 같은 기기에 푸시가 여러 번 간다
    @Column(name = "token", nullable = false, length = 255, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    // 같은 기기에서 다른 계정으로 로그인하면 토큰 주인이 바뀐다
    public void reassign(Long userId, DeviceType deviceType) {
        this.userId = userId;
        this.deviceType = deviceType;
    }
}
