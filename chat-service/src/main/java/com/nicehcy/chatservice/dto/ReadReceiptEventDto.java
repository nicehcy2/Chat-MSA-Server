package com.nicehcy.chatservice.dto;

/**
 * 읽음 이벤트. Kafka 페이로드이자 각 노드가 구독자에게 내보내는 WebSocket 페이로드다.
 *
 * 서버는 "누가 어디까지 읽었는지"만 알리고, 메시지별 안 읽은 수는 클라이언트가
 * 각 멤버의 워터마크를 모아 직접 계산한다. 메시지 수만큼 숫자를 다시 실어보내지 않아도 되므로
 * 페이로드가 작고, 방에 메시지가 아무리 쌓여도 전파 비용이 늘지 않는다.
 */
public record ReadReceiptEventDto(

        Long chatRoomId,
        Long userId, // 읽은 사람
        String lastReadMessageId // TSID. JS 정밀도 문제로 문자열
) {
}
