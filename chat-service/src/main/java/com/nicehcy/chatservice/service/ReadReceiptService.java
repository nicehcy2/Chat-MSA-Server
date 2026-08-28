package com.nicehcy.chatservice.service;

import com.nicehcy.chatservice.dto.ReadReceiptEventDto;
import com.nicehcy.chatservice.messaging.producer.ReadReceiptKafkaProducer;
import com.nicehcy.chatservice.repository.ChatRoomMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReadReceiptService {

    private final ChatRoomMembershipRepository chatRoomMembershipRepository;
    private final ReadReceiptKafkaProducer readReceiptKafkaProducer;

    /**
     * 읽음 워터마크를 전진시키고, 실제로 전진했을 때만 다른 참여자에게 알린다.
     *
     * @param chatRoomId        읽은 채팅방 ID (STOMP 경로변수)
     * @param userId            읽은 사용자 ID (CONNECT 시 JWT에서 추출해 세션에 저장한 값)
     * @param lastReadMessageId 여기까지 읽었다고 알리는 메시지의 TSID
     */
    public void markAsRead(final Long chatRoomId, final Long userId, final String lastReadMessageId) {

        final long messageId;
        try {
            messageId = Long.parseLong(lastReadMessageId);
        } catch (NumberFormatException e) {
            log.warn("메시지 ID 형식이 잘못된 읽음 요청 - 무시 [roomId: {}, userId: {}, messageId: {}]",
                    chatRoomId, userId, lastReadMessageId);
            return;
        }

        // 단일 UPDATE로 원자적으로 처리된다. 중복/역행 이벤트는 0행 갱신으로 걸러진다.
        final int updatedRows = chatRoomMembershipRepository.updateLastReadMessageId(chatRoomId, userId, messageId);

        if (updatedRows == 0) {
            // 이미 더 뒤까지 읽었거나(중복·역행), 이 방의 참여 중인 멤버가 아니다.
            log.debug("워터마크 전진 없음 - 브로드캐스트 스킵 [roomId: {}, userId: {}]", chatRoomId, userId);
            return;
        }

        // 리포지토리 메서드가 자체 트랜잭션이라 이 시점엔 이미 커밋된 상태다.
        readReceiptKafkaProducer.publish(new ReadReceiptEventDto(chatRoomId, userId, lastReadMessageId));
        log.info("읽음 처리 완료 [roomId: {}, userId: {}, lastReadMessageId: {}]", chatRoomId, userId, lastReadMessageId);
    }
}
