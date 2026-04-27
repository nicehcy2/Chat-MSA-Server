package com.nicehcy.chatservice.messaging.consumer;

import com.nicehcy.chatservice.dto.MessageDto;
import com.nicehcy.chatservice.dto.PushNotificationRequestDto;
import com.nicehcy.chatservice.entity.ChatRoomMembership;
import com.nicehcy.chatservice.messaging.producer.ChatKafkaProducer;
import com.nicehcy.chatservice.repository.ChatRoomMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatKafkaProducer chatKafkaProducer;

    @Value("${ONLINE_KEY_PREFIX}") private String ONLINE_KEY_PREFIX;
    @Value("${CHAT_NODE_ID}") private String chatNodeId;

    // 다중 채팅 서버 적용 시 각 채팅 서버마다 groupId를 다르게 설정해야 한다.
    @KafkaListener(topics = "${CHAT_TOPIC:chat-topic}", groupId = "${CHAT_NODE_ID}")
    public void listenKafkaChatMessage(@Payload final MessageDto messageDto) {

        log.info("[5/6] Kafka 리스너 메시지 수신 [{}]", messageDto.id());

        // TODO:멱등성 확인

        // 해당 채팅방에 모든 멤버 조회
        List<ChatRoomMembership> memberships = chatRoomMembershipRepository.findByChatRoomId(messageDto.chatRoomId());
        List<Long> userIds = memberships.stream()
                .map(ChatRoomMembership::getUserId)
                // .filter(uid -> !uid.equals(messageDto.senderId()))
                .toList();

        if (userIds.isEmpty()) return;

        List<String> redisKeys = userIds.stream()
                .map(uid -> ONLINE_KEY_PREFIX + uid)
                .toList();

        List<String> onlineInfos = redisTemplate.opsForValue().multiGet(redisKeys); // MGET

        int n = Math.min(userIds.size(), onlineInfos.size());
        List<Long> onlines = new ArrayList<>(), offlines = new ArrayList<>();
        for (int i = 0; i < n; i++) {

            // 값이 null이면 키 없음 = 오프라인
            if (onlineInfos.get(i) != null) onlines.add(userIds.get(i));
            else offlines.add(userIds.get(i));
        }

        if (!onlines.isEmpty()) {
            final String destination = "/sub/chatroom" + messageDto.chatRoomId();
            messagingTemplate.convertAndSend(destination, messageDto);
            log.info("[6/6] STOMP over WebSocket을 통해 메시지 전송");
        }

        // offlines가 비어있으면 아예 produce 안 하도록
        if (!offlines.isEmpty()) {
            // 푸시알림 토픽으로 카프카 메시지 전송
            PushNotificationRequestDto pushDto = new PushNotificationRequestDto(messageDto, offlines);
            chatKafkaProducer.producePushNotification(pushDto);
        }
    }
}
