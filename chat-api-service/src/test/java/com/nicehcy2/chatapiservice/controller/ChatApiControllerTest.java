package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.config.JwtAuthConverter;
import com.nicehcy2.chatapiservice.dto.CreateChatRoomRequestDto;
import com.nicehcy2.chatapiservice.service.ChatApiService;
import com.nicehcy2.chatapiservice.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 계층 검증: 요청이 실제 스프링 MVC 파이프라인(역직렬화 → @Valid → 핸들러 → 예외 리졸버)을 타는지.
 * 핸들러 메서드를 직접 호출하는 단위 테스트로는 @ExceptionHandler 라우팅과 @Valid 부착 여부를 검증할 수 없다.
 */
@WebMvcTest(controllers = ChatApiController.class)
@AutoConfigureMockMvc(addFilters = false) // 인증은 게이트웨이 담당. 시큐리티 필터는 이 테스트 관심사가 아님
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
class ChatApiControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ChatApiService chatApiService;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean JwtAuthConverter jwtAuthConverter; // SecurityConfig가 슬라이스에 포함될 경우를 대비
    // 메인 클래스의 @EnableJpaAuditing이 슬라이스에도 딸려와 JPA 메타모델을 요구한다. 웹 계층 테스트라 JPA는 불필요
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    static final String VALID_BODY = """
            {
              "title": "무지출 챌린지",
              "description": "하루 만원으로 살기",
              "maxParticipants": 10,
              "isPrivate": false,
              "ageGroups": ["TWENTIES"],
              "jobGroups": ["EMPLOYEE"],
              "dailyLimit": 10000
            }
            """;

    @Test
    void 정상_요청은_201과_생성된_chatRoomId를_반환한다() throws Exception {
        when(chatRoomService.createChatRoom(eq(7L), any(CreateChatRoomRequestDto.class))).thenReturn(10L);

        mockMvc.perform(post("/api/chats")
                        .header("X-User-Id", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string("10"));
    }

    @Test
    void Valid_실패는_400이고_fieldErrors에_필드명이_담긴다() throws Exception {
        // 컨트롤러에 @Valid가 빠지면 서비스까지 도달해버린다 → 서비스 호출 여부까지 확인
        String bodyWithBlankTitle = VALID_BODY.replace("\"무지출 챌린지\"", "\"   \"");

        mockMvc.perform(post("/api/chats")
                        .header("X-User-Id", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithBlankTitle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());

        verify(chatRoomService, never()).createChatRoom(any(), any());
    }

    @Test
    void enum_오타_등_역직렬화_실패는_500이_아니라_400이다() throws Exception {
        // Jackson 단계에서 터지는 HttpMessageNotReadableException은 @Valid보다 먼저 발생한다.
        // GlobalExceptionHandler의 400 목록에 없으면 catch-all이 500으로 만들어버린다.
        String bodyWithTypo = VALID_BODY.replace("\"TWENTIES\"", "\"TWENTIS\"");

        mockMvc.perform(post("/api/chats")
                        .header("X-User-Id", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithTypo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));

        verify(chatRoomService, never()).createChatRoom(any(), any());
    }

    @Test
    void X_User_Id_헤더가_없으면_400이다() throws Exception {
        mockMvc.perform(post("/api/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest());

        verify(chatRoomService, never()).createChatRoom(any(), any());
    }
}
