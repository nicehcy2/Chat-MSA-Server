package com.nicehcy.chatservice.dto.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicehcy.chatservice.dto.MessageDto;

public class MessagePayloadConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(MessageDto messageDto) {

        try {
            return objectMapper.writeValueAsString(messageDto);
        } catch (Exception e) {
            throw new IllegalArgumentException("Message 직렬화 실패", e);
        }
    }

    public static MessageDto toMessageDto(String json) {

        try {
            return objectMapper.readValue(json, MessageDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("MessageDto 역직렬화 실패", e);
        }
    }
}
