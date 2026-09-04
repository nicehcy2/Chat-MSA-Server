package com.nicehcy2.chatapiservice.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 핸들러 메서드를 직접 호출해 응답 형태를 검증한다. (스프링 컨텍스트 없음)
 */
class GlobalExceptionHandlerTest {

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void Valid_실패는_400이고_fieldErrors에_필드명과_메시지가_담긴다() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "제목은 1~18자여야 합니다."));
        bindingResult.addError(new FieldError("request", "password", "비밀번호는 숫자 4자리여야 합니다."));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
        MethodArgumentNotValidException e = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(e);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ResponseCode._BAD_REQUEST.getCode(), body.code());
        assertEquals(2, body.fieldErrors().size());
        assertTrue(body.fieldErrors().contains(new FieldErrorDto("title", "제목은 1~18자여야 합니다.")));
        assertTrue(body.fieldErrors().contains(new FieldErrorDto("password", "비밀번호는 숫자 4자리여야 합니다.")));
    }

    @Test
    void fieldErrors를_가진_GeneralException은_그대로_응답에_실린다() {
        // 서비스 레벨 필드 간 규칙("비공개면 비밀번호 필수")용
        GeneralException e = new GeneralException(ResponseCode._BAD_REQUEST,
                List.of(new FieldErrorDto("password", "비공개 방은 비밀번호가 필요합니다.")));

        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(e);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.fieldErrors().size());
        assertEquals("password", body.fieldErrors().get(0).field());
    }

    @Test
    void fieldErrors가_없는_GeneralException은_fieldErrors가_null이다() {
        GeneralException e = new GeneralException(ResponseCode.CHATROOM_NOT_FOUND);

        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(e);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("CHATROOM404", body.code());
        assertNull(body.fieldErrors()); // 검증 에러가 아니면 항목 자체를 내려주지 않는다
    }

    @Test
    void 역직렬화_실패는_400이다() {
        // enum 오타("TWENTIS") 등 Jackson 단계에서 터지는 예외.
        // @Valid보다 먼저 발생해 FieldError가 없으므로 필드명 없이 400만 준다. (catch-all 500으로 새면 안 됨)
        HttpMessageNotReadableException e = new HttpMessageNotReadableException(
                "Cannot deserialize value of type AgeGroup from String \"TWENTIS\"",
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(e);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ResponseCode._BAD_REQUEST.getCode(), body.code());
        assertNull(body.fieldErrors());
    }

    @SuppressWarnings("unused")
    void dummy(String arg) { }
}
