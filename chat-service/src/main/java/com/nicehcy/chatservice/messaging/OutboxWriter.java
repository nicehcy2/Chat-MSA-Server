package com.nicehcy.chatservice.messaging;

import com.nicehcy.chatservice.dto.MessageResponseDto;
import com.nicehcy.chatservice.dto.converter.MessagePayloadConverter;
import com.nicehcy.chatservice.entity.Outbox;
import com.nicehcy.chatservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 채팅 메시지 발행 기록. 호출자의 트랜잭션 안에서 실행되어 메시지 저장과 함께 커밋되거나 함께 롤백된다.
 * Debezium이 aggregate_type으로 토픽을 정한다: {aggregateType}-topic
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    static final String AGGREGATE_TYPE = "chat";
    static final String EVENT_TYPE = "MESSAGE_SENT";

    private final OutboxRepository outboxRepository;

    public void messageSent(MessageResponseDto message) {
        outboxRepository.save(new Outbox(
                AGGREGATE_TYPE, String.valueOf(message.chatRoomId()), EVENT_TYPE, MessagePayloadConverter.toJson(message)));
    }
}
