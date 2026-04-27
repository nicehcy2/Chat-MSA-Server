package com.nicehcy2.dto;

import com.nicehcy2.entity.enums.DeviceType;

public record FcmTokenRequestDto(
        Long userId,
        String fcmToken,
        DeviceType deviceType
) {
}
