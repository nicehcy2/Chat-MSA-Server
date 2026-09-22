package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 소유자는 X-User-Id 헤더로 정한다. body로 받으면 남의 토큰을 등록할 수 있다
public record FcmTokenRequestDto(
        @NotBlank String fcmToken,
        @NotNull DeviceType deviceType
) {
}
