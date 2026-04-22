package com.nicehcy2.pushnotificationservice.service;

import com.google.firebase.messaging.*;
import com.nicehcy2.pushnotificationservice.dto.MessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class FcmService implements PushNotificationService {

    /*
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${FCM_TOKEN_PREFIX:fcm:token:}")
    private String FCM_TOKEN_PREFIX;

    /**
     * 오프라인 유저들에게 FCM 푸시 알림 전송
     *
     * @param offlineUserIds 오프라인 유저 ID 목록
     * @param messageDto     전송할 메시지 정보
     *
    public void sendPushToOfflineUsersLegacy(List<Long> offlineUserIds, MessageDto messageDto) {

        // 오프라인 유저들의 FCM 토큰 조회 (Redis MGET)
        List<String> redisKeys = offlineUserIds.stream()
                .map(uid -> FCM_TOKEN_PREFIX + uid)
                .toList();

        List<String> fcmTokens = redisTemplate.opsForValue().multiGet(redisKeys);

        if (fcmTokens == null || fcmTokens.isEmpty()) {
            log.info("FCM 토큰 없음 - 푸시 전송 스킵");
            return;
        }

        // null 토큰 제거 (토큰 미등록 유저)
        List<String> validTokens = fcmTokens.stream()
                .filter(Objects::nonNull)
                .toList();

        if (validTokens.isEmpty()) {
            log.info("유효한 FCM 토큰 없음 - 푸시 전송 스킵");
            return;
        }

        // FCM 메시지 구성
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(validTokens)
                .setNotification(Notification.builder()
                        .setTitle("새 메시지")
                        .setBody(messageDto.content())
                        .build())
                // 프론트에서 활용할 수 있는 데이터 페이로드
                .putData("chatRoomId", String.valueOf(messageDto.chatRoomId()))
                .putData("senderId", String.valueOf(messageDto.senderId()))
                .putData("messageId", String.valueOf(messageDto.id()))
                .putData("messageType", messageDto.messageType())
                .build();

        // FCM 전송
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 전송 완료 - 성공: {}, 실패: {}", response.getSuccessCount(), response.getFailureCount());

            // 실패한 토큰 처리
            handleFailedTokens(response, validTokens, offlineUserIds);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패: {}", e.getMessage());
        }
    }
    */


    @Override
    public void sendPushToOfflineUsers(MessageDto messageDto, List<String> fcmTokens) {

        // Redis에서 먼저 가져오기
        // 없는것은 DB에서

        // FCM 메시지 구성
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(fcmTokens)
                .setNotification(Notification.builder()
                        .setTitle("새 메시지")
                        .setBody(messageDto.content())
                        .build())
                // 프론트에서 활용할 수 있는 데이터 페이로드
                .putData("chatRoomId", String.valueOf(messageDto.chatRoomId()))
                .putData("senderId", String.valueOf(messageDto.senderId()))
                .putData("messageId", String.valueOf(messageDto.id()))
                .putData("messageType", messageDto.messageType())
                .build();

        // FCM 전송
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 전송 완료 - 성공: {}, 실패: {}",
                    response.getSuccessCount(), response.getFailureCount());

            // 실패한 토큰 처리
            // handleFailedTokens(response, validTokens, offlineUserIds);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 만료/유효하지 않은 FCM 토큰 Redis에서 삭제
     */
    private void handleFailedTokens(BatchResponse response,
                                    List<String> tokens,
                                    List<Long> userIds) {

        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {

                MessagingErrorCode errorCode = responses.get(i).getException().getMessagingErrorCode();

                // 등록 해제된 토큰이면 Redis에서 삭제
                if (errorCode == MessagingErrorCode.UNREGISTERED) {
                    //redisTemplate.delete(FCM_TOKEN_PREFIX + userIds.get(i));
                    log.warn("만료된 FCM 토큰 삭제 - userId: {}", userIds.get(i));
                } else {
                    log.warn("FCM 전송 실패 - userId: {}, errorCode: {}", userIds.get(i), errorCode);
                }
            }
        }
    }
}
