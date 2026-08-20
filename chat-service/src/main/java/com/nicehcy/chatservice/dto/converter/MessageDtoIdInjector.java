package com.nicehcy.chatservice.dto.converter;

import com.github.f4b6a3.tsid.TsidCreator;
import com.nicehcy.chatservice.dto.MessageResponseDto;
import com.nicehcy.chatservice.dto.MessageSendRequestDto;

import java.time.LocalDateTime;

public class MessageDtoIdInjector {

    public static MessageResponseDto withGeneratedMessageId(final MessageSendRequestDto messageSendRequest, final Long senderId) {

        return MessageResponseDto.builder()
                .messageTSID(String.valueOf(TsidCreator.getTsid().toLong())) //TSID 기반 ID 생성기, 시간에 따라 증가하는 값을 가지며 최신 데이터일수록 더 큰 uniqute한 ID가 생성된다.
                .correlationId(String.valueOf(messageSendRequest.correlationId()))
                .chatRoomId(messageSendRequest.chatRoomId())
                .senderId(senderId)
                .messageType(messageSendRequest.messageType())
                .content(messageSendRequest.content())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
