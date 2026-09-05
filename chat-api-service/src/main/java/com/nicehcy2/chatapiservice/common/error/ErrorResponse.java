package com.nicehcy2.chatapiservice.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDto> fieldErrors
) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
