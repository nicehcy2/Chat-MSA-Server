package com.nicehcy.chatservice.dto;

import java.util.List;

public record PushNotificationRequestDto (

    MessageDto messageDto,
    List<Long> targetOfflineUserIds
) {}
