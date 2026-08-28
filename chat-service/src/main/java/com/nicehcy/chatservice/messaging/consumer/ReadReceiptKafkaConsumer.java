package com.nicehcy.chatservice.messaging.consumer;

import com.nicehcy.chatservice.dto.ReadReceiptEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadReceiptKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 리스너와 같은 이유로 노드마다 다른 groupId를 쓴다.
     * 모든 노드가 이벤트를 받아 자기 노드에 붙어 있는 세션에 각자 뿌려야 하기 때문이다.
     */
    @KafkaListener(
            topics = "${CHAT_READ_TOPIC:chat-read-topic}",
            groupId = "${CHAT_NODE_ID}-read",
            containerFactory = "readReceiptListenerContainerFactory")
    public void listenKafkaReadReceipt(@Payload final ReadReceiptEventDto event) {

        /*
         * 읽음 이벤트는 워터마크라 같은 값을 여러 번 받아도 결과가 같다.
         * 채팅 메시지 리스너와 달리 Redis 멱등성 가드를 두지 않는 이유다.
         */
        final String destination = "/sub/chatroom" + event.chatRoomId() + ".read";
        messagingTemplate.convertAndSend(destination, event);

        log.info("읽음 이벤트 전파 [roomId: {}, userId: {}, lastReadMessageId: {}]",
                event.chatRoomId(), event.userId(), event.lastReadMessageId());
    }
}
