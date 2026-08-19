package com.nicehcy.chatservice.messaging.consumer;

import com.nicehcy.chatservice.config.socket.SocketConnectionTracker;
import com.nicehcy.chatservice.dto.MessageResponseDto;
import com.nicehcy.chatservice.entity.ChatRoomMembership;
import com.nicehcy.chatservice.entity.FcmToken;
import com.nicehcy.chatservice.repository.ChatRoomMembershipRepository;
import com.nicehcy.chatservice.repository.FcmTokenRepository;
import com.nicehcy.chatservice.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationConsumer {

    private final ChatRoomMembershipRepository chatRoomMembershipRepository;
    private final SocketConnectionTracker socketConnectionTracker;
    private final RedisTemplate<String, String> redisTemplate;
    private final FcmTokenRepository fcmTokenRepository;
    private final PushNotificationService pushNotificationService;

    @Value("${IDEMPOTENCY_TTL_DAYS:1}") private long idempotencyTtlDays;

    // 채팅 리스너(노드별 groupId)와 달리 모든 노드가 공유하는 groupId를 사용한다.
    // 메시지당 한 노드만 푸시를 처리하게 되어 중복 발송이 방지된다.
    @KafkaListener(topics = "${CHAT_TOPIC:chat-topic}", groupId = "${PUSH_GROUP_ID:push-notification-group}")
    public void listenKafkaPushNotificationRecord(@Payload final MessageResponseDto messageDto) {

        log.info("푸시 알림 Kafka 리스너 메시지 수신 [{}]", messageDto.messageTSID());

        // 채팅방 멤버 중 발신자를 제외한 유저가 푸시 대상 후보
        List<Long> userIds = chatRoomMembershipRepository.findByChatRoomId(messageDto.chatRoomId())
                .stream()
                .map(ChatRoomMembership::getUserId)
                .filter(uid -> !uid.equals(messageDto.senderId()))
                .toList();

        if (userIds.isEmpty()) return;

        List<Long> offlineUserIds = socketConnectionTracker.filterOfflineUserIds(userIds);
        if (offlineUserIds.isEmpty()) return;

        // 처음에는 Redis에서 조회
        // Redis에 없으면 DB 조회
        // 같은 기기의 토큰이 중복 등록돼 있어도 한 번만 발송되도록 distinct 처리
        List<String> fcmTokens = fcmTokenRepository.findByUserUserIdIn(offlineUserIds)
                .stream()
                .map(FcmToken::getToken)
                .distinct()
                .toList();

        if (fcmTokens.isEmpty()) {
            log.info("유효한 FCM 토큰 없음 - 푸시 전송 스킵 [{}]", messageDto.messageTSID());
            return;
        }

        // 리밸런싱/재전달로 같은 메시지를 다시 받아도 중복 발송되지 않도록 가드.
        // 발송 직전에 두어야 앞 단계(조회) 실패가 재시도로 복구된다. 맨 앞에 두면 재시도가 스킵되어 푸시가 유실된다.
        if (!tryMarkPushSent(messageDto.messageTSID())) {
            log.debug("중복 푸시 스킵: {}", messageDto.messageTSID());
            return;
        }

        pushNotificationService.sendPushToOfflineUsers(messageDto, fcmTokens);
    }

    // Redis SET NX: 키가 없으면 등록(true) + 발송 진행, 이미 있으면(false) 중복으로 스킵
    private boolean tryMarkPushSent(String messageId) {
        try {
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent("push:" + messageId, "1", Duration.ofDays(idempotencyTtlDays));
            return Boolean.TRUE.equals(isNew);
        } catch (Exception e) {
            log.warn("푸시 멱등성 체크 실패 - 중복 허용하고 진행: {}", e.getMessage());
            return true; // Redis 장애 시 푸시 유실보다 중복을 택한다
        }
    }
}
