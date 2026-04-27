package com.nicehcy2.pushnotificationservice.dto;

import java.util.List;

public record PushNotificationRequestDto (

        MessageDto messageDto,
        List<Long> targetOfflineUserIds
) {}
