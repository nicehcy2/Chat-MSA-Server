package com.nicehcy2.chatapiservice.dto.converter;

import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.entity.Message;

public class MessageDtoConverter {

    public static MessageDto toMessageDto(final Message message) {

        return MessageDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .unreadCount(message.getUnreadCount())
                .build();
    }
}
