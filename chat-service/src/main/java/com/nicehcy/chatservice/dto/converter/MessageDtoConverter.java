package com.nicehcy.chatservice.dto.converter;

import com.nicehcy.chatservice.dto.MessageResponseDto;
import com.nicehcy.chatservice.entity.Message;

public class MessageDtoConverter {

    public static Message toMessage(final MessageResponseDto messageDto) {

        return Message.builder()
                .id(messageDto.messageTSID())
                .chatRoomId(messageDto.chatRoomId())
                .senderId(messageDto.senderId())
                .messageType(messageDto.messageType())
                .content(messageDto.content())
                .timestamp(messageDto.timestamp())
                .unreadCount(messageDto.unreadCount())
                .build();
    }
}
