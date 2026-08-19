package com.nicehcy.chatservice.service;


import com.nicehcy.chatservice.dto.MessageResponseDto;

import java.util.List;

public interface PushNotificationService {

    void sendPushToOfflineUsers(MessageResponseDto messageDto, List<String> fcmTokens);
}
