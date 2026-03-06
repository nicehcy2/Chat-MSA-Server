package com.nicehcy.chatservice.service;

import com.nicehcy.chatservice.dto.MessageDto;
import com.nicehcy.chatservice.dto.converter.MessageDtoConverter;
import com.nicehcy.chatservice.dto.converter.MessagePayloadConverter;
import com.nicehcy.chatservice.entity.Outbox;
import com.nicehcy.chatservice.repository.MessageRepository;
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
    private final MessageRepository messageRepository;

    @Transactional
    public void sendMessage(final MessageDto messageDto) {

        log.info("[1/4] 메시지 전송 프로세스 시작");

        // 메시지 DTO에 ID(TSID) 추가
        MessageDto message = generateMessageID(messageDto);
        log.info("[2/4] TSID 기반 메시지 ID 생성 완료: {}", message.id());

        // chatdb Message 테이블에 저장
        messageRepository.save(MessageDtoConverter.toMessage(messageDto));
        log.info("[3/4] 채팅 메시지 저장 완료 - chatRoomId: {}, senderId: {}", message.chatRoomId(), message.senderId());

        // outbox 저장소에 저장
        saveMessageToOutbox(message);
        log.info("[4/4] 메시지 Outbox 저장 완료 (chatRoomId: {}, senderId: {})", message.chatRoomId(), message.senderId());
    }

    private void saveMessageToOutbox(MessageDto messageDto) {

        Outbox outbox = new Outbox("CHAT",  messageDto.id(),"MESSAGE_SENT", MessagePayloadConverter.toJson(messageDto));
        outboxRepository.save(outbox);
    }
}
