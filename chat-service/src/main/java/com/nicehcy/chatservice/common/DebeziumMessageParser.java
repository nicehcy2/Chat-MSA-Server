package com.nicehcy.chatservice.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicehcy.chatservice.dto.MessageDto;
import com.nicehcy.chatservice.dto.converter.MessagePayloadConverter;

public class DebeziumMessageParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static MessageDto parse(String kafkaMessageJson) throws Exception {

        JsonNode root = objectMapper.readTree(kafkaMessageJson);
        JsonNode payload = root.get("payload");

        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("payload is missing or null");
        }

        String op = payload.path("op").asText("");
        if (!"c".equals(op)) return null; // INSERT만 처리

        JsonNode row = payload.path("after");
        if (row.isMissingNode() || row.isNull()) return null;

        String messagePayload = reqText(row, "message_dto_payload");
        return MessagePayloadConverter.toMessageDto(messagePayload);
    }

    private static String reqText(JsonNode n, String f) {
        JsonNode v = n.get(f);
        if (v == null || v.isNull()) throw new IllegalArgumentException("missing field: " + f);
        return v.asText();
    }
}