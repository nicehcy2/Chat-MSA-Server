package com.nicehcy.chatservice.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MessageType {
    TEXT("TEXT"), ENTER("ENTER"), EXIT("EXIT"), IMAGE("IMAGE");

    private final String value;

    public static MessageType of(String value) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.value.equals(value)) {
                return messageType;
            }
        }
        throw new IllegalArgumentException();
    }
}