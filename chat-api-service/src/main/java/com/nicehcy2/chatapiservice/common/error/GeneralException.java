package com.nicehcy2.chatapiservice.common.error;

import lombok.Getter;

import java.util.List;

/**
 * 비즈니스 로직에서 발생하는 커스텀 예외 클래스
 *
 * RuntimeException을 상속받아 언체크 예외로 동작
 * → try-catch 없이 예외를 던질 수 있고, GlobalExceptionHandler가 자동으로 처리
 *
 * 사용 예시:
 * throw new GeneralException(ResponseCode.CHATROOM_ACCESS_DENIED);
 * → httpStatus: 403, code: "CHATROOM403", message: "채팅방 멤버가 아닙니다."
 */
@Getter
public class GeneralException extends RuntimeException {

    // 에러 코드 (httpStatus, code, message를 포함한 enum)
    private final ResponseCode errorCode;
    private final List<FieldErrorDto> fieldErrors;

    // 기본 생성자 - 에러코드 없이 던질 때, 자동으로 500 처리
    public GeneralException() {
        super(ResponseCode._INTERNAL_SERVER_ERROR.getMessage());
        this.errorCode = ResponseCode._INTERNAL_SERVER_ERROR;
        this.fieldErrors = List.of();
    }

    // 특정 에러코드 지정
    public GeneralException(ResponseCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
    }

    // 특정 에러코드 + 커스텀 메시지
    public GeneralException(ResponseCode errorCode, String message) {
        super(errorCode.getMessage(message));
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
    }

    // 특정 에러코드 + 원인 예외 (스택트레이스 보존)
    public GeneralException(ResponseCode errorCode, Throwable cause) {
        super(errorCode.getMessage(cause), cause);
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
    }

    public GeneralException(ResponseCode errorCode, List<FieldErrorDto> fieldErrors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }
}
