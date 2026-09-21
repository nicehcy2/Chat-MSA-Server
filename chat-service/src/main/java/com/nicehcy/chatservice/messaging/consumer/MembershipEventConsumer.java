package com.nicehcy.chatservice.messaging.consumer;

import com.nicehcy.chatservice.config.socket.SocketConnectionTracker;
import com.nicehcy.chatservice.config.socket.WebSocketSessionRegistry;
import com.nicehcy.chatservice.dto.MembershipEventDto;
import com.nicehcy.chatservice.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventConsumer {

    // 클라이언트가 일반 끊김과 구분하는 close 코드. reason에 방 id를 실어 그 방 화면에서만 빠져나가게 한다
    private static final int KICKED_CLOSE_CODE = 4403;

    private final SystemMessageService systemMessageService;
    private final SocketConnectionTracker socketConnectionTracker;
    private final WebSocketSessionRegistry sessionRegistry;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${IDEMPOTENCY_TTL_DAYS:1}") private long idempotencyTtlDays;

    /**
     * 노드마다 다른 groupId를 쓴다. 강퇴 시 모든 노드가 자기 세션을 각자 끊어야 하기 때문이다.
     * 시스템 메시지는 한 번만 저장돼야 하므로 노드와 무관한 eventId 키로 가드한다.
     */
    @KafkaListener(
            topics = "${CHAT_MEMBERSHIP_TOPIC:chat-membership-topic}",
            groupId = "${CHAT_NODE_ID}-membership",
            containerFactory = "membershipListenerContainerFactory")
    public void listen(@Payload final MembershipEventDto event) {

        if (event.type() == MembershipEventDto.MembershipEventType.KICKED) {
            sessionRegistry.close(socketConnectionTracker.localSessionIds(event.userId()),
                    new CloseStatus(KICKED_CLOSE_CODE, String.valueOf(event.chatRoomId())));
        }

        if (!tryMarkAsProcessed(event.eventId())) {
            return;
        }
        switch (event.type()) {
            case JOINED -> systemMessageService.memberJoined(event.chatRoomId(), event.userId());
            case LEFT -> systemMessageService.memberLeft(event.chatRoomId(), event.userId());
            case KICKED -> systemMessageService.memberKicked(event.chatRoomId(), event.userId());
        }
        log.info("멤버십 이벤트 처리 [{} room={} user={}]", event.type(), event.chatRoomId(), event.userId());
    }

    private boolean tryMarkAsProcessed(String eventId) {
        try {
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent("processed:membership:" + eventId, "1", Duration.ofDays(idempotencyTtlDays));
            return Boolean.TRUE.equals(isNew);
        } catch (Exception e) {
            log.warn("Redis 멱등성 체크 실패 - 중복 허용하고 처리 진행: {}", e.getMessage());
            return true;
        }
    }
}
