package com.nicehcy.chatservice.dto.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nicehcy.chatservice.dto.MessageResponseDto;

public class MessagePayloadConverter {

    // LocalDateTime을 ISO-8601 문자열로 직렬화해야 Debezium expand.json.payload와 컨슈머 역직렬화가 깨지지 않는다.
    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public static String toJson(MessageResponseDto messageDto) {

        try {
            return objectMapper.writeValueAsString(messageDto);
        } catch (Exception e) {
            throw new IllegalArgumentException("Message 직렬화 실패", e);
        }
    }

    public static MessageResponseDto toMessageResponseDto(String json) {

        try {
            return objectMapper.readValue(json, MessageResponseDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("MessageResponseDto 역직렬화 실패", e);
        }
    }
}
