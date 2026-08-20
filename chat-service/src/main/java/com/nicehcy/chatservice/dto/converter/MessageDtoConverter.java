package com.nicehcy.chatservice.dto.converter;

import com.nicehcy.chatservice.dto.MessageResponseDto;
import com.nicehcy.chatservice.entity.Message;

public class MessageDtoConverter {

    public static Message toMessage(final MessageResponseDto messageDto) {

        return Message.builder()
                // DTO는 JS 정밀도 때문에 문자열로 들고 있지만, DB에는 원래 타입인 long으로 저장한다.
                .id(Long.parseLong(messageDto.messageTSID()))
                .chatRoomId(messageDto.chatRoomId())
                .senderId(messageDto.senderId())
                .messageType(messageDto.messageType())
                .content(messageDto.content())
                .timestamp(messageDto.timestamp())
                .build();
    }
}
