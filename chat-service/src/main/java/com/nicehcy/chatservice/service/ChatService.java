package com.nicehcy.chatservice.service;

import com.nicehcy.chatservice.dto.MessageResponseDto;
import com.nicehcy.chatservice.dto.MessageSendRequestDto;
import com.nicehcy.chatservice.dto.converter.MessageDtoConverter;
import com.nicehcy.chatservice.dto.converter.MessagePayloadConverter;
import com.nicehcy.chatservice.entity.Outbox;
import com.nicehcy.chatservice.repository.MessageRepository;
import com.nicehcy.chatservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nicehcy.chatservice.dto.converter.MessageDtoIdInjector.withGeneratedMessageId;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final OutboxRepository outboxRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public void sendMessage(final MessageSendRequestDto messageSendRequest, final Long senderId) {

        log.info("[1/4] 메시지 전송 프로세스 시작");

        // 요청 DTO를 TSID/타임스탬프가 채워진 응답 DTO로 변환
        final MessageResponseDto messageDtoWithId = withGeneratedMessageId(messageSendRequest, senderId);
        log.info("[2/4] TSID 기반 메시지 ID 생성 완료: {}", messageDtoWithId.messageTSID());

        /**
         * 같은 RDBMS에 저장되기 때문에 하나라도 저장에 실패할 경우, 롤백이 된다.
         * Transactional 어노테이션을 통해 트랜잭션 원자성이 보장된다.
         */
        // chatdb Message 테이블에 저장
        messageRepository.save(MessageDtoConverter.toMessage(messageDtoWithId));
        log.info("[3/4] 채팅 메시지 저장 완료 - chatRoomId: {}, senderId: {}", messageDtoWithId.chatRoomId(), messageDtoWithId.senderId());
        // outbox 저장소에 저장
        saveMessageToOutbox(messageDtoWithId);
        log.info("[4/4] 메시지 Outbox 저장 완료 (chatRoomId: {}, senderId: {})", messageDtoWithId.chatRoomId(), messageDtoWithId.senderId());
    }

    private void saveMessageToOutbox(MessageResponseDto messageDto) {

        Outbox outbox = new Outbox("CHAT",  messageDto.chatRoomId().toString(),"MESSAGE_SENT", MessagePayloadConverter.toJson(messageDto));
        outboxRepository.save(outbox);
    }
}
