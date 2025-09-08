package com.nicehcy2.chatapiservice.dto;

import lombok.Builder;

@Builder
public record ChatServerInfoResponse(
        String nodeId,
        String websocketUrl
) {
}
