package com.nicehcy.chatservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicehcy.chatservice.dto.MessageDto;

// TODO: 리팩토링 필수
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

        return MessageDto.builder()
                .id(reqText(row, "message_id"))
                .chatRoomId(row.path("chat_room_id").asLong())
                .senderId(row.path("sender_id").asLong())
                .messageType(reqText(row, "message_type"))
                .content(optText(row, "content"))
                .timestamp(optText(row, "timestamp")) // 문자열/마이크로초 모두 올 수 있음
                .unreadCount(row.path("unread_count").asInt(0))
                .publishRetryCount(row.hasNonNull("publish_retry_count") ? row.get("publish_retry_count").asInt() : null)
                .saveStatus(row.hasNonNull("save_status") ? row.get("save_status").asBoolean() : null)
                .build();
    }

    private static String reqText(JsonNode n, String f) {
        JsonNode v = n.get(f);
        if (v == null || v.isNull()) throw new IllegalArgumentException("missing field: " + f);
        return v.asText();
    }
    private static String optText(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}