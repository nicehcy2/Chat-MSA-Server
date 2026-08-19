package com.nicehcy2.entity;

import com.nicehcy2.common.BaseEntity;
import com.nicehcy2.entity.enums.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FCM 토큰은 기기+앱 설치당 하나이므로 유니크. 중복 행이 쌓이면 같은 기기에 푸시가 여러 번 간다.
    @Column(name = "token", nullable = false, length = 255, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    /**
     * 이미 등록된 토큰의 소유자/기기 정보를 갱신합니다.
     * 같은 기기에서 다른 계정으로 로그인하면 토큰 주인이 바뀔 수 있습니다.
     */
    public void reassign(User user, DeviceType deviceType) {
        this.user = user;
        this.deviceType = deviceType;
    }
}
