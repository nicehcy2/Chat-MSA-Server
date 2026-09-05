package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.common.error.GeneralException;
import com.nicehcy2.chatapiservice.common.error.ResponseCode;
import com.nicehcy2.chatapiservice.config.JwtAuthConverter;
import com.nicehcy2.chatapiservice.dto.ChatRoomDetailDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomHostDto;
import com.nicehcy2.chatapiservice.dto.MembershipStatus;
import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.JobGroup;
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

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/chats/{id}/detail — 응답 JSON 형태가 프론트 계약(RoomDetailSheet)과 맞는지.
 */
@WebMvcTest(controllers = ChatApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
class ChatApiControllerDetailTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ChatApiService chatApiService;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean JwtAuthConverter jwtAuthConverter;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    static final ChatRoomDetailDto SAMPLE = ChatRoomDetailDto.builder()
            .chatRoomId(10L).title("무지출 챌린지").description("하루 만원")
            .participationCount(3).maxParticipants(10).dailyLimit(10_000)
            .isPrivate(true).imageUrl(null)
            .ageGroups(Set.of(AgeGroup.TWENTIES)).jobGroups(Set.of())
            .createdAt(LocalDateTime.of(2026, 9, 1, 12, 0))
            .host(new ExploreRoomHostDto(11L, "티끌모아태산", null, AgeGroup.THIRTIES, JobGroup.EMPLOYEE))
            .membershipStatus(MembershipStatus.LEFT)
            .build();

    @Test
    void 정상_요청은_200이고_멤버십_상태는_enum_문자열로_비밀번호는_없이_내려간다() throws Exception {
        when(chatApiService.getChatRoomDetail(10L, 7L)).thenReturn(SAMPLE);

        mockMvc.perform(get("/api/chats/10/detail").header("X-User-Id", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(10))
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andExpect(jsonPath("$.membershipStatus").value("LEFT"))
                .andExpect(jsonPath("$.ageGroups[0]").value("TWENTIES"))
                .andExpect(jsonPath("$.host.nickname").value("티끌모아태산"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void 방이_없으면_404와_도메인_코드로_응답한다() throws Exception {
        when(chatApiService.getChatRoomDetail(10L, 7L))
                .thenThrow(new GeneralException(ResponseCode.CHATROOM_NOT_FOUND));

        mockMvc.perform(get("/api/chats/10/detail").header("X-User-Id", 7L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ResponseCode.CHATROOM_NOT_FOUND.getCode()));
    }

    @Test
    void X_User_Id_헤더가_없으면_400이다() throws Exception {
        // 멤버십 상태가 요청자 기준이라 헤더 없이는 응답을 만들 수 없다
        mockMvc.perform(get("/api/chats/10/detail"))
                .andExpect(status().isBadRequest());

        verify(chatApiService, never()).getChatRoomDetail(any(), any());
    }
}
