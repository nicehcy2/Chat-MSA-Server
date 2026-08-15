package com.nicehcy.chatservice.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.nicehcy.chatservice.dto.MessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmService implements PushNotificationService {

    @Override
    public void sendPushToOfflineUsers(MessageDto messageDto, List<String> fcmTokens) {

        // 서비스 계정 키 없이 부팅된 환경(로컬 등)에서는 FirebaseApp이 초기화되지 않는다.
        // 이때 FirebaseMessaging.getInstance()가 IllegalStateException을 던져
        // Kafka 리스너 재시도를 유발하므로 여기서 스킵한다.
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase 미초기화 - 푸시 전송 스킵 [{}]", messageDto.id());
            return;
        }

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
