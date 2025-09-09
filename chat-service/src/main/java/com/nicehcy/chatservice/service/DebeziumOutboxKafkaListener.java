package com.nicehcy.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebeziumOutboxKafkaListener {

    private final ChatService chatService;

    @KafkaListener(topics = "${OUTBOX_KAFKA_TOPIC}", groupId = "${CHAT_NODE_ID}")
    public void sendTest(ConsumerRecord<String, String> record) throws Exception {

        chatService.saveMessage(DebeziumMessageParser.parse(record.value()));
    }
}
