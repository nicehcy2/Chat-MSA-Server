package com.nicehcy2.pushnotificationservice.service;

import com.nicehcy2.pushnotificationservice.dto.MessageDto;

import java.util.List;

public interface PushNotificationService {

    void sendPushToOfflineUsers(MessageDto messageDto, List<String> fcmTokens);
}
