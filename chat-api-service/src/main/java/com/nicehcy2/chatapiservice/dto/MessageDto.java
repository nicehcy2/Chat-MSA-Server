package com.nicehcy2.chatapiservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nicehcy2.chatapiservice.entity.enums.MessageType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MessageDto(

        // TSID는 long이지만 JS Number의 안전 정수 범위(2^53)를 넘기 때문에 JSON에는 문자열로 내보낸다.
        // 자바 쪽에서 Long으로 두면 조회 쿼리에서 타입 변환 없이 그대로 받을 수 있다.
        @JsonSerialize(using = ToStringSerializer.class)
        Long messageTSID,
        Long chatRoomId, // 목적지(전달할 그룹 채팅방) ID
        Long senderId, // 발신인 ID
        MessageType messageType, // 메시지 타입(텍스트, 사진, 영수증)
        String content, // 메시지 내용
        LocalDateTime timestamp, // 타임스탬프
        String senderImageUrl,
        String nickname
) {
}
