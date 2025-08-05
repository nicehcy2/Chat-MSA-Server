package com.nicehcy.chatservice.service;

import com.nicehcy.chatservice.dto.MessageDto;
import com.nicehcy.chatservice.entity.Outbox;
import com.nicehcy.chatservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nicehcy.chatservice.dto.converter.MessageDtoIdInjector.generateMessageID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public void sendMessage(MessageDto messageDto) {

        log.info("[1/5] 메시지 전송 프로세스 시작");

        // 메시지 DTO에 ID(TSID) 추가
        MessageDto message = generateMessageID(messageDto);
        log.info("[2/5] TSID 기반 메시지 ID 생성 완료: {}", message.id());

        // outbox 저장소에 저장
        saveMessageToOutbox(message);
        log.info("[3/5] 메시지 Outbox 저장 완료 (chatRoomId: {}, senderId: {})", message.chatRoomId(), message.senderId());

        // 카프카 메시지 전송 로직
        /*
        MessageCorrelationData messageCorrelationData = new MessageCorrelationData(messageDto.id(), messageDto);
        rabbitTemplate.convertAndSend(CHAT_EXCHANGE_NAME, "chat.room." + messageDto.chatRoomId(), message,
                msg -> { // MessagePostProcessor로 메시지를 보내기 전 메시지를 가공
                    msg.getMessageProperties().setExpiration("5000"); // expiration을 설정해서 메시지 만료 설정(TTL 설정)
                    return msg;
                },
                messageCorrelationData);

         */
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
