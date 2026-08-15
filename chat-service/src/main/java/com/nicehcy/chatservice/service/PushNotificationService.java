package com.nicehcy.chatservice.service;


import com.nicehcy.chatservice.dto.MessageDto;

import java.util.List;

public interface PushNotificationService {

    void sendPushToOfflineUsers(MessageDto messageDto, List<String> fcmTokens);
}
