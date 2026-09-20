package com.nicehcy2.chatapiservice.messaging;

import com.nicehcy2.chatapiservice.dto.event.MembershipEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 멤버십 변화를 커밋 뒤에 Kafka로 발행한다.
 * 진실은 DB의 멤버십 행이고 이벤트는 파생 효과(시스템 메시지, 세션 종료)를 위한 알림이라
 * Outbox 없이 보낸다. 롤백된 변경은 발행되지 않고, 브로커 장애 시 유실은 감수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventPublisher {

    @Value("${CHAT_MEMBERSHIP_TOPIC:chat-membership-topic}")
    private String topic;

    private final KafkaTemplate<String, MembershipEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(MembershipEvent event) {

        kafkaTemplate.send(topic, String.valueOf(event.chatRoomId()), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        log.error("멤버십 이벤트 발행 실패 [{} room={} user={}]", event.type(), event.chatRoomId(), event.userId(), e);
                    }
                });
    }
}
