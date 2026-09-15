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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/chats/{id}/leave — 본문 없는 204 응답과 예외 매핑.
 */
@WebMvcTest(controllers = ChatApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
class ChatApiControllerLeaveTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ChatApiService chatApiService;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean JwtAuthConverter jwtAuthConverter;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void 정상_요청은_204이고_본문이_없다() throws Exception {
        mockMvc.perform(post("/api/chats/10/leave").header("X-User-Id", 7L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(chatRoomService).leaveChatRoom(7L, 10L);
    }

    @Test
    void 활성_멤버가_아니면_403과_도메인_코드로_응답한다() throws Exception {
        doThrow(new GeneralException(ResponseCode.CHATROOM_ACCESS_DENIED))
                .when(chatRoomService).leaveChatRoom(7L, 10L);

        mockMvc.perform(post("/api/chats/10/leave").header("X-User-Id", 7L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.CHATROOM_ACCESS_DENIED.getCode()));
    }

    @Test
    void X_User_Id_헤더가_없으면_400이다() throws Exception {
        mockMvc.perform(post("/api/chats/10/leave"))
                .andExpect(status().isBadRequest());

        verify(chatRoomService, never()).leaveChatRoom(any(), any());
    }
}
