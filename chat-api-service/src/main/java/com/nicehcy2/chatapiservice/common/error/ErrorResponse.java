package com.nicehcy2.chatapiservice.common.error;

public record ErrorResponse(
        String code,
        String message
) { }
