package com.nicehcy.chatservice.messaging.consumer;

import com.nicehcy.chatservice.common.DebeziumMessageParser;
import com.nicehcy.chatservice.dto.MessageDto;
import com.nicehcy.chatservice.entity.Outbox;
import com.nicehcy.chatservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class DebeziumOutboxKafkaListener {

    private final KafkaTemplate<String, MessageDto> kafkaTemplate;
    private final OutboxRepository outboxRepository;
    @Value("${KAFKA_TOPIC:chat-topic}") private String kafkaTopic;

    @KafkaListener(
            topics = "${OUTBOX_KAFKA_TOPIC}",
            groupId = "${OUTBOX_GROUP_ID}",
            properties = {
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "auto.offset.reset=earliest"
            }
    )
    public void sendMessage(ConsumerRecord<String, String> record) throws Exception {

        MessageDto messageDto = DebeziumMessageParser.parse(record.value());
        if (messageDto == null) {
            log.debug("스킵 (INSERT 아님 또는 after 없음)");
            return;
        }
        log.info("[5/6] Kafka 리스너 수신 메시지: {}", messageDto.id());

        // TODO: PENDING인것만 처리

        Outbox outbox = outboxRepository.findByAggregateId(messageDto.id())
                .orElseThrow(() -> new IllegalArgumentException("해당 messageID를 가진 Outbox 레코드가 존재하지 않습니다: " + messageDto.id()));

        try {
            final String chatRoomId = messageDto.chatRoomId().toString();
            kafkaTemplate.send(kafkaTopic, chatRoomId, messageDto)
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Kafka 전송 실패 - topic: {}, chatRoomId: {}, error: {}", kafkaTopic, chatRoomId, ex.getMessage());
                                    outbox.markFailed();
                                } else {
                                    log.info("Kafka 전송 성공 - topic: {}, chatRoomId: {}, offset: {}", kafkaTopic, chatRoomId, result.getRecordMetadata().offset());
                                    outbox.markPublished();
                                }
                                outboxRepository.save(outbox);
                            });

        } catch (Exception e) {
            log.error("카프카 전송 과정에서 예외 발생: {}", e.getMessage());
            outbox.markFailed();
            outboxRepository.save(outbox);
        }

    }

    // TODO: 실패한 것들을 주기적으로 재시도
}
