package com.nicehcy.chatservice.messaging.producer;

import com.nicehcy.chatservice.dto.ReadReceiptEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadReceiptKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${CHAT_READ_TOPIC:chat-read-topic}") private String chatReadTopic;

    /**
     * 채팅 메시지와 달리 Outbox/Debezium을 태우지 않고 Kafka로 직접 발행한다.
     *
     * 읽음 이벤트는 누적 최대값(워터마크)이라 한 건이 유실돼도 다음 이벤트가 덮어쓴다.
     * 반면 발생 빈도는 메시지의 참여자 수 배라, Outbox에 전부 INSERT하면
     * 정작 유실되면 안 되는 메시지 파이프라인이 읽음 트래픽에 잠식된다.
     */
    public void publish(final ReadReceiptEventDto event) {

        // 파티션 키를 chatRoomId로 두어 같은 방의 읽음 이벤트끼리는 순서가 보장되게 한다.
        kafkaTemplate.send(chatReadTopic, String.valueOf(event.chatRoomId()), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        // 다음 읽음 이벤트가 복구해주므로 예외를 던져 사용자 요청을 실패시키지 않는다.
                        log.warn("읽음 이벤트 발행 실패 [roomId: {}, userId: {}]: {}",
                                event.chatRoomId(), event.userId(), exception.getMessage());
                    }
                });
    }
}
