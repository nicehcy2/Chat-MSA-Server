package com.nicehcy.chatservice.service;

import com.nicehcy.chatservice.dto.MessageDto;
import com.nicehcy.chatservice.entity.Outbox;
import com.nicehcy.chatservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nicehcy.chatservice.dto.converter.MessageDtoIdInjector.generateMessageID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, MessageDto> kafkaTemplate;

    private final String KAFKA_TOPIC = "chat-topic";

    @Transactional
    public void sendMessage(final MessageDto messageDto) {

        log.info("[1/6] 메시지 전송 프로세스 시작");

        // 메시지 DTO에 ID(TSID) 추가
        MessageDto message = generateMessageID(messageDto);
        log.info("[2/6] TSID 기반 메시지 ID 생성 완료: {}", message.id());

        // outbox 저장소에 저장
        saveMessageToOutbox(message);
        log.info("[3/6] 메시지 Outbox 저장 완료 (chatRoomId: {}, senderId: {})", message.chatRoomId(), message.senderId());

        // 카프카 메시지 전송 로직
        final String chatRoomId = message.chatRoomId().toString();
        kafkaTemplate.send(KAFKA_TOPIC, chatRoomId, message);
        log.info("[4/6] Kafka에 채팅 메시지 전송 - topic: {}, chatRoomId: {}, messageId: {}", KAFKA_TOPIC, chatRoomId, message.id());
    }

    private void saveMessageToOutbox(MessageDto messageDto) {

        Outbox outbox = Outbox.builder()
                .messageId(messageDto.id())
                .chatRoomId(messageDto.chatRoomId())
                .senderId(messageDto.senderId())
                .messageType(messageDto.messageType())
                .content(messageDto.content())
                .timestamp(messageDto.timestamp())
                .unreadCount(messageDto.unreadCount())
                .publishRetryCount(messageDto.publishRetryCount())
                .saveStatus(messageDto.saveStatus())
                .build();

        outboxRepository.save(outbox);
    }
}
