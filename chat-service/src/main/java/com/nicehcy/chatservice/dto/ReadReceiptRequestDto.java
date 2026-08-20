package com.nicehcy.chatservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 클라이언트가 "이 메시지까지 읽었다"고 알릴 때 보내는 페이로드.
 */
public record ReadReceiptRequestDto(

        // TSID는 자바에서 long이지만 JS Number의 안전 정수 범위(2^53)를 넘기 때문에 문자열로 주고받는다.
        @NotBlank String lastReadMessageId
) {
}
