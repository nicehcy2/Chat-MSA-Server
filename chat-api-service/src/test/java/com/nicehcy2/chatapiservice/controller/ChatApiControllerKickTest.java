package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.common.error.GeneralException;
import com.nicehcy2.chatapiservice.common.error.ResponseCode;
import com.nicehcy2.chatapiservice.config.JwtAuthConverter;
import com.nicehcy2.chatapiservice.service.ChatApiService;
import com.nicehcy2.chatapiservice.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DELETE /api/chats/{id}/members/{userId} — 경로 변수 바인딩, 204 응답, 예외 매핑.
 */
@WebMvcTest(controllers = ChatApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
class ChatApiControllerKickTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ChatApiService chatApiService;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean JwtAuthConverter jwtAuthConverter;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void 정상_요청은_204이고_요청자와_방과_대상이_서비스에_전달된다() throws Exception {
        mockMvc.perform(delete("/api/chats/10/members/8").header("X-User-Id", 7L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(chatRoomService).kickMember(7L, 10L, 8L);
    }

    @Test
    void 호스트가_아니면_403과_도메인_코드로_응답한다() throws Exception {
        doThrow(new GeneralException(ResponseCode.CHATROOM_NOT_HOST))
                .when(chatRoomService).kickMember(7L, 10L, 8L);

        mockMvc.perform(delete("/api/chats/10/members/8").header("X-User-Id", 7L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.CHATROOM_NOT_HOST.getCode()));
    }

    @Test
    void X_User_Id_헤더가_없으면_400이다() throws Exception {
        mockMvc.perform(delete("/api/chats/10/members/8"))
                .andExpect(status().isBadRequest());

        verify(chatRoomService, never()).kickMember(any(), any(), any());
    }
}
