package com.nicehcy.chatservice.dto.converter;

import com.github.f4b6a3.tsid.TsidCreator;
import com.nicehcy.chatservice.dto.MessageDto;

import java.time.LocalDateTime;

public class MessageDtoIdInjector {

    public static MessageDto generateMessageID(MessageDto messageDto) {

        return MessageDto.builder()
                .id(String.valueOf(TsidCreator.getTsid().toLong())) // TSID ID 생성기, 시간에 따라 ID에 영향이 가고 최신 데이터일수록 ID 값이 커진다.
                .chatRoomId(messageDto.chatRoomId())
                .senderId(messageDto.senderId())
                .messageType(messageDto.messageType())
                .content(messageDto.content())
                .timestamp(String.valueOf(LocalDateTime.now()))
                .unreadCount(0)
                .publishRetryCount(0)
                .saveStatus(false)
                .build();
    }
}
