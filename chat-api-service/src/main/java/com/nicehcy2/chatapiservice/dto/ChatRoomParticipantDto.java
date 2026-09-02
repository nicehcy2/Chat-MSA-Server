package com.nicehcy2.chatapiservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record ChatRoomParticipantDto(

        Long userId,
        String nickname,
        String imageUrl,
        Boolean isHost,

        @JsonSerialize(using = ToStringSerializer.class)
        Long lastReadMessageId
) {
}
