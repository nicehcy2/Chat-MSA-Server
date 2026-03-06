package com.nicehcy.chatservice.service;

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
        // 스냅샷을 건너뛰려면 다음 줄 주석 해제
        // if ("r".equals(op)) return null;

        // c/u 는 after, d 는 before
        JsonNode row = "d".equals(op) ? payload.path("before") : payload.path("after");
        if (row.isMissingNode() || row.isNull()) return null;

        String messagePayload = reqText(row, "payload");
        return MessagePayloadConverter.toMessageDto(messagePayload);
    }

    private static String reqText(JsonNode n, String f) {
        JsonNode v = n.get(f);
        if (v == null || v.isNull()) throw new IllegalArgumentException("missing field: " + f);
        return v.asText();
    }
}