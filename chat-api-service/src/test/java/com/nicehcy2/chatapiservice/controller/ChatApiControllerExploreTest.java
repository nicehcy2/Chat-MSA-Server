package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.config.JwtAuthConverter;
import com.nicehcy2.chatapiservice.dto.ExploreChatRoomRequestDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomHostDto;
import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import com.nicehcy2.chatapiservice.service.ChatApiService;
import com.nicehcy2.chatapiservice.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/chats/explore — 쿼리스트링이 요청 DTO에 바인딩되고 응답 JSON 형태가 프론트 계약과 맞는지.
 */
@WebMvcTest(controllers = ChatApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
class ChatApiControllerExploreTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ChatApiService chatApiService;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean JwtAuthConverter jwtAuthConverter;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    static final ExploreRoomDto SAMPLE = new ExploreRoomDto(
            10L, "무지출 챌린지", "하루 만원", 3, 10, 10_000,
            true, null, Set.of(AgeGroup.TWENTIES), Set.of(),
            false, LocalDateTime.of(2026, 9, 1, 12, 0),
            new ExploreRoomHostDto(11L, "티끌모아태산", null, AgeGroup.THIRTIES, JobGroup.EMPLOYEE));

    @Test
    void 조건_없이_호출하면_200이고_서비스에_빈_조건을_전달한다() throws Exception {
        when(chatApiService.exploreChatRooms(eq(7L), any())).thenReturn(List.of(SAMPLE));

        mockMvc.perform(get("/api/chats/explore").header("X-User-Id", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].chatRoomId").value(10))
                .andExpect(jsonPath("$[0].isPrivate").value(true))
                .andExpect(jsonPath("$[0].isBanned").value(false))
                .andExpect(jsonPath("$[0].ageGroups[0]").value("TWENTIES"))
                .andExpect(jsonPath("$[0].host.nickname").value("티끌모아태산"))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        ArgumentCaptor<ExploreChatRoomRequestDto> captor = ArgumentCaptor.forClass(ExploreChatRoomRequestDto.class);
        verify(chatApiService).exploreChatRooms(eq(7L), captor.capture());
        ExploreChatRoomRequestDto request = captor.getValue();
        assertNull(request.q());
        assertNull(request.ageGroup());
        assertNull(request.jobGroup());
        assertNull(request.before());
        assertNull(request.limit());
    }

    @Test
    void 검색어_필터_커서_limit이_서비스에_전달된다() throws Exception {
        when(chatApiService.exploreChatRooms(eq(7L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/chats/explore")
                        .header("X-User-Id", 7L)
                        .param("q", "무지출")
                        .param("ageGroup", "TWENTIES")
                        .param("jobGroup", "EMPLOYEE")
                        .param("before", "87")
                        .param("limit", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<ExploreChatRoomRequestDto> captor = ArgumentCaptor.forClass(ExploreChatRoomRequestDto.class);
        verify(chatApiService).exploreChatRooms(eq(7L), captor.capture());
        ExploreChatRoomRequestDto request = captor.getValue();
        assertEquals("무지출", request.q());
        assertEquals(AgeGroup.TWENTIES, request.ageGroup());
        assertEquals(JobGroup.EMPLOYEE, request.jobGroup());
        assertEquals(87L, request.before());
        assertEquals(10, request.limit());
    }

    @Test
    void ageGroup_오타는_400이고_fieldErrors에_필드명이_담기며_내부_클래스명은_노출되지_않는다() throws Exception {
        // @ModelAttribute 바인딩의 typeMismatch는 스프링 기본 메시지에 java 클래스명이 들어 있어 핸들러가 고정 문구로 바꾼다
        mockMvc.perform(get("/api/chats/explore")
                        .header("X-User-Id", 7L)
                        .param("ageGroup", "TWENTIS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("ageGroup"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(not(containsString("java."))))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(not(containsString("Failed to convert"))));

        verify(chatApiService, never()).exploreChatRooms(any(), any());
    }

    @Test
    void 검색어가_50자를_넘으면_400이고_서비스를_호출하지_않는다() throws Exception {
        // 컨트롤러에 @Valid가 빠지면 서비스까지 도달한다
        mockMvc.perform(get("/api/chats/explore")
                        .header("X-User-Id", 7L)
                        .param("q", "가".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("q"));

        verify(chatApiService, never()).exploreChatRooms(any(), any());
    }

    @Test
    void limit이_범위_밖이면_400이다() throws Exception {
        // 범위 판정은 서비스가 하고(IllegalArgumentException), 핸들러가 400으로 매핑하는지만 본다
        when(chatApiService.exploreChatRooms(eq(7L), any())).thenThrow(new IllegalArgumentException("limit"));

        mockMvc.perform(get("/api/chats/explore")
                        .header("X-User-Id", 7L)
                        .param("limit", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void X_User_Id_헤더가_없으면_400이다() throws Exception {
        mockMvc.perform(get("/api/chats/explore"))
                .andExpect(status().isBadRequest());

        verify(chatApiService, never()).exploreChatRooms(any(), any());
    }
}
