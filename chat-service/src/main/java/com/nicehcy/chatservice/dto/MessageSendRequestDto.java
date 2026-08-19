package com.nicehcy.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.nicehcy.chatservice.entity.enums.MessageType;
import java.util.UUID;

public record MessageSendRequestDto(

        @NotNull @Positive Long chatRoomId, // 목적지(전달할 그룹 채팅방) ID
        @NotNull UUID correlationId, // 전송 확인을 위해 클라이언트가 생성한 UUID
        @NotNull MessageType messageType, // 메시지 타입(텍스트, 사진, 영수증)
        @NotBlank @Size(max = 32768) String content // 메시지 내용
) {
}
