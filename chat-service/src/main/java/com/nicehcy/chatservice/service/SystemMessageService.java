package com.nicehcy.chatservice.service;

import com.github.f4b6a3.tsid.TsidCreator;
import com.nicehcy.chatservice.dto.MessageResponseDto;
import com.nicehcy.chatservice.dto.converter.MessageDtoConverter;
import com.nicehcy.chatservice.entity.enums.MessageType;
import com.nicehcy.chatservice.messaging.OutboxWriter;
import com.nicehcy.chatservice.repository.MessageRepository;
import com.nicehcy.chatservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 참여·나가기·강퇴 시스템 메시지. 일반 메시지와 같은 경로(Message + Outbox → Debezium → chat-topic)로 전파된다.
 * 멤버십 검사와 발신자 워터마크 갱신은 하지 않는다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SystemMessageService {

    private final MessageRepository messageRepository;
    private final OutboxWriter outboxWriter;
    private final UserRepository userRepository;

    public void memberJoined(Long chatRoomId, Long userId) {
        write(chatRoomId, userId, "님이 참여했습니다");
    }

    public void memberLeft(Long chatRoomId, Long userId) {
        write(chatRoomId, userId, "님이 나갔습니다");
    }

    public void memberKicked(Long chatRoomId, Long userId) {
        write(chatRoomId, userId, "님이 내보내졌습니다");
    }

    // senderId는 행위자다. 히스토리 조회가 User와 inner join이라 null이면 시스템 메시지가 결과에서 빠진다
    private void write(Long chatRoomId, Long userId, String suffix) {

        String nickname = userRepository.findById(userId)
                .map(user -> user.getNickname())
                .orElse(null);
        if (nickname == null) {
            log.warn("시스템 메시지 스킵 - 유저 없음 [userId: {}, chatRoomId: {}]", userId, chatRoomId);
            return;
        }

        MessageResponseDto message = MessageResponseDto.builder()
                .messageTSID(String.valueOf(TsidCreator.getTsid().toLong()))
                .correlationId(null)
                .chatRoomId(chatRoomId)
                .senderId(userId)
                .messageType(MessageType.SYSTEM)
                .content(nickname + suffix)
                .timestamp(LocalDateTime.now())
                .build();

        messageRepository.save(MessageDtoConverter.toMessage(message));
        outboxWriter.messageSent(message);
    }
}
