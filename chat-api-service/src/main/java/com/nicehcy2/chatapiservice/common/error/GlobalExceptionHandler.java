package com.nicehcy2.chatapiservice.common.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 전역 예외 처리 클래스
 *
 * @RestControllerAdvice
 * - 모든 @RestController에서 발생하는 예외를 한 곳에서 처리
 * - 예외 발생 시 각 컨트롤러마다 try-catch를 작성할 필요 없이 이 클래스에서 중앙 관리
 *
 * 처리 우선순위 (예외 타입 기반으로 매칭)
 * 1. 잘못된 요청 계열 → 400
 * 2. GeneralException (커스텀) → ResponseCode에 정의된 상태코드
 * 3. Exception (나머지 모든 예외) → 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // IllegalArgumentException/IllegalStateException: 잘못된 인자·상태
    // ServletRequestBindingException: 필수 헤더/파라미터 누락 (예: X-User-Id 없음)
    // MethodArgumentTypeMismatchException: 경로/파라미터 타입 불일치 (예: chatRoomId에 문자열)
    // HttpMessageNotReadableException: 요청 바디 역직렬화 실패 (예: enum 오타)
    // → 뒤의 셋은 원래 스프링이 기본으로 400 처리하지만, 아래 Exception.class 핸들러가
    //   먼저 가로채 500으로 만들어버리므로 여기서 명시적으로 400에 매핑한다.
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            ServletRequestBindingException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception e) {

        log.warn("Bad request exception: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ResponseCode._BAD_REQUEST.getCode(),
                        ResponseCode._BAD_REQUEST.getMessage() // 내부 정보 노출 방지를 위해 고정 메시지 사용
                ));
    }

    // @Valid 실패는 어떤 필드가 왜 틀렸는지 fieldErrors로 내려준다
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

        List<FieldErrorDto> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldErrorDto(f.getField(), f.getDefaultMessage()))
                .toList();

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ResponseCode._BAD_REQUEST.getCode(),
                        ResponseCode._BAD_REQUEST.getMessage(),
                        fieldErrors
                ));
    }

    // unique 위반(같은 유저의 동시 join 등). 트랜잭션이 이미 rollback-only라 서비스에서 삼킬 수 없다
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {

        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        ResponseCode._CONFLICT.getCode(),
                        ResponseCode._CONFLICT.getMessage()
                ));
    }

    // GeneralException: 비즈니스 로직에서 의도적으로 던진 커스텀 예외
    // ResponseCode에 정의된 httpStatus, code, message를 그대로 사용
    @ExceptionHandler({
            GeneralException.class,
    })
    public ResponseEntity<ErrorResponse> handleGeneralException(GeneralException e) {

        log.warn("General exception: {}", e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(new ErrorResponse(
                        e.getErrorCode().getCode(),
                        e.getMessage(),
                        e.getFieldErrors().isEmpty() ? null : e.getFieldErrors()
                ));
    }

    // 위에서 처리되지 않은 모든 예외의 최후 처리
    // e.getMessage()는 내부 정보가 노출될 수 있으므로 고정 메시지 사용
    @ExceptionHandler({
            Exception.class
    })
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        // 500 에러는 반드시 로깅 (원인 추적을 위해 스택트레이스 포함)
        log.error("Unhandled exception occurred", e);

        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        ResponseCode._INTERNAL_SERVER_ERROR.getCode(),
                        ResponseCode._INTERNAL_SERVER_ERROR.getMessage()
                ));
    }
}
