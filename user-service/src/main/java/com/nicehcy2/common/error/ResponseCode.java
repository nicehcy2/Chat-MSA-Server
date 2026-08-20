package com.nicehcy2.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * API 응답 코드 관리 enum
 *
 * 모든 에러 코드를 한 곳에서 중앙 관리
 * 새로운 에러 추가 시 이 파일에만 추가하면 됨
 *
 * 구성요소:
 * - httpStatus: 실제 HTTP 상태코드 (404, 500 등)
 * - code: 클라이언트가 에러를 식별하는 문자열 ("USER404")
 * - message: 사람이 읽을 수 있는 에러 메시지
 */
@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    // 정상 code
    OK(HttpStatus.OK, "200", "Ok"),

    // Common Error
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "요청 값을 확인해주세요."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다. 로그인 후 이용해주세요."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다."),
    _METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "허용되지 않는 요청 방식입니다."),

    // User Error
    // 로그인 실패는 이메일 부재/비밀번호 불일치를 구분하지 않는다. (계정 존재 여부 열거 방지)
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH401", "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER409", "이미 존재하는 사용자입니다."),

    // JWT Error
    JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401", "유효하지 않은 토큰입니다."),
    JWT_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4011", "만료된 토큰입니다."),
    JWT_UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4012", "지원하지 않는 토큰입니다."),
    JWT_MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4013", "잘못된 형식의 토큰입니다."),
    JWT_MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4014", "토큰이 존재하지 않습니다."),

    // Redis(Session) Error
    SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "SESSION401", "세션이 존재하지 않습니다."),
    SESSION_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "SESSION4011", "토큰 재사용이 감지되어 세션이 만료되었습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "SESSION4012", "세션이 만료되었습니다. 다시 로그인해주세요."),

    // FCM Error
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM404", "존재하지 않는 FCM 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String code; // 클라이언트 식별용 코드 (예: "USER404")
    private final String message; // 기본 에러 메시지

    /**
     * 예외와 함께 메시지를 반환
     * 기본 메시지에 예외 메시지를 붙여서 반환
     */
    public String getMessage(Throwable e) {

        return this.getMessage(this.message + " - " + e.getMessage());
    }

    /**
     * 메시지가 null이거나 blank면 기본 메시지(this.message)로 fallback
     */
    public String getMessage(String message) {

        return Optional.ofNullable(message)
                .filter(Predicate.not(String::isBlank))
                .orElse(this.getMessage());
    }
}
