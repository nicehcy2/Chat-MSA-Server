package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.common.error.GeneralException;
import com.nicehcy2.chatapiservice.common.error.ResponseCode;
import com.nicehcy2.chatapiservice.dto.ChatRoomHostDto;
import com.nicehcy2.chatapiservice.dto.ExploreChatRoomRequestDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomHostDto;
import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import com.nicehcy2.chatapiservice.repository.ChatRoomMembershipRepository;
import com.nicehcy2.chatapiservice.repository.ChatRoomRepository;
import com.nicehcy2.chatapiservice.repository.MessageRepository;
import com.nicehcy2.chatapiservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatApiServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock ChatRoomMembershipRepository chatRoomMembershipRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ChatApiServiceImpl chatApiService;

    static final Long REQUESTER_ID = 7L;
    static final int MAX_PAGE_SIZE = 50;
    static final int DEFAULT_PAGE_SIZE = 20;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatApiService, "maxExplorePageSize", MAX_PAGE_SIZE);
    }

    /**
     * 둘러보기·검색은 엔드포인트 하나. 필터·정렬·커서는 리포지토리 쿼리가 맡고(ChatRoomExploreRepositoryTest),
     * 여기서는 파라미터 정규화와 응답 조립(호스트, 강퇴 여부)만 검증한다.
     */
    @Nested
    class 방_둘러보기 {

        final ExploreChatRoomRequestDto noCondition = ExploreChatRoomRequestDto.builder().build();

        ChatRoom room(long id, String password) {
            ChatRoom room = ChatRoom.builder()
                    .title("방" + id)
                    .description("설명" + id)
                    .maxParticipants(10)
                    .participationCount(3)
                    .dailyLimit(10_000)
                    .password(password)
                    .imageUrl(null)
                    .ageGroups(Set.of(AgeGroup.TWENTIES))
                    .jobGroups(Set.of())
                    .build();
            room.setId(id);
            return room;
        }

        ChatRoomHostDto host(long chatRoomId, long userId) {
            return new ChatRoomHostDto(chatRoomId, userId, "호스트" + userId, null, AgeGroup.THIRTIES, JobGroup.EMPLOYEE);
        }

        void stubUserExists() {
            when(userRepository.existsById(REQUESTER_ID)).thenReturn(true);
        }

        void stubRooms(ChatRoom... rooms) {
            when(chatRoomRepository.findForExplore(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(rooms));
        }

        // ----- 거부 -----

        @Test
        void 존재하지_않는_유저면_404이고_방을_조회하지_않는다() {
            when(userRepository.existsById(REQUESTER_ID)).thenReturn(false);

            GeneralException e = assertThrows(GeneralException.class,
                    () -> chatApiService.exploreChatRooms(REQUESTER_ID, noCondition));

            assertEquals(ResponseCode.USER_NOT_FOUND, e.getErrorCode());
            verify(chatRoomRepository, never()).findForExplore(any(), any(), any(), any(), any());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, MAX_PAGE_SIZE + 1})
        void limit이_범위를_벗어나면_400이고_방을_조회하지_않는다(int limit) {
            // 메시지 API와 동일하게 IllegalArgumentException → GlobalExceptionHandler가 400으로 매핑
            stubUserExists();

            assertThrows(IllegalArgumentException.class,
                    () -> chatApiService.exploreChatRooms(REQUESTER_ID, noCondition.toBuilder().limit(limit).build()));

            verify(chatRoomRepository, never()).findForExplore(any(), any(), any(), any(), any());
        }

        // ----- 파라미터 정규화 -----

        @Test
        void limit이_없으면_기본_20개를_요청한다() {
            stubUserExists();
            stubRooms();

            chatApiService.exploreChatRooms(REQUESTER_ID, noCondition);

            verify(chatRoomRepository).findForExplore(isNull(), isNull(), isNull(), isNull(),
                    eq(PageRequest.of(0, DEFAULT_PAGE_SIZE)));
        }

        @Test
        void limit_최대값은_허용한다() {
            stubUserExists();
            stubRooms();

            chatApiService.exploreChatRooms(REQUESTER_ID, noCondition.toBuilder().limit(MAX_PAGE_SIZE).build());

            verify(chatRoomRepository).findForExplore(any(), any(), any(), any(), eq(PageRequest.of(0, MAX_PAGE_SIZE)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void 검색어가_비어있으면_검색_조건_없이_조회한다(String q) {
            stubUserExists();
            stubRooms();

            chatApiService.exploreChatRooms(REQUESTER_ID, noCondition.toBuilder().q(q).build());

            verify(chatRoomRepository).findForExplore(isNull(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        void 검색어_앞뒤_공백은_잘라서_전달한다() {
            stubUserExists();
            stubRooms();

            chatApiService.exploreChatRooms(REQUESTER_ID, noCondition.toBuilder().q("  무지출  ").build());

            verify(chatRoomRepository).findForExplore(eq("무지출"), any(), any(), any(), any(Pageable.class));
        }

        @ParameterizedTest
        @CsvSource({
                "100%,   100!%",
                "a_b,    a!_b",
                "!,      !!",
                "50%_!,  50!%!_!!"
        })
        void 검색어의_LIKE_와일드카드는_이스케이프해서_전달한다(String raw, String escaped) {
            // %·_는 LIKE 패턴 문자라 문자 그대로 찾으려면 이스케이프가 필요하다. 이스케이프 문자는 '!'(쿼리의 ESCAPE '!'와 짝)
            stubUserExists();
            stubRooms();

            chatApiService.exploreChatRooms(REQUESTER_ID, noCondition.toBuilder().q(raw).build());

            verify(chatRoomRepository).findForExplore(eq(escaped), any(), any(), any(), any(Pageable.class));
        }

        @Test
        void 필터와_커서는_그대로_전달한다() {
            stubUserExists();
            stubRooms();
            ExploreChatRoomRequestDto request = ExploreChatRoomRequestDto.builder()
                    .ageGroup(AgeGroup.TWENTIES).jobGroup(JobGroup.EMPLOYEE).before(87L).limit(10).build();

            chatApiService.exploreChatRooms(REQUESTER_ID, request);

            verify(chatRoomRepository).findForExplore(
                    isNull(), eq(AgeGroup.TWENTIES), eq(JobGroup.EMPLOYEE), eq(87L), eq(PageRequest.of(0, 10)));
        }

        // ----- 응답 조립 -----

        @Test
        void 조건에_맞는_방이_없으면_빈_리스트이고_호스트_강퇴_조회는_하지_않는다() {
            stubUserExists();
            stubRooms();

            List<ExploreRoomDto> result = chatApiService.exploreChatRooms(REQUESTER_ID, noCondition);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(chatRoomMembershipRepository, never()).findHostsByChatRoomIds(anyCollection());
            verify(chatRoomMembershipRepository, never()).findBannedChatRoomIds(any());
        }

        @Test
        void 방_정보와_호스트가_응답에_매핑된다() {
            stubUserExists();
            ChatRoom room = room(10L, null);
            LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 12, 0);
            ReflectionTestUtils.setField(room, "createdAt", createdAt);
            stubRooms(room);
            when(chatRoomMembershipRepository.findHostsByChatRoomIds(anyCollection())).thenReturn(List.of(host(10L, 11L)));
            when(chatRoomMembershipRepository.findBannedChatRoomIds(REQUESTER_ID)).thenReturn(List.of());

            ExploreRoomDto dto = chatApiService.exploreChatRooms(REQUESTER_ID, noCondition).get(0);

            assertEquals(10L, dto.chatRoomId());
            assertEquals("방10", dto.title());
            assertEquals("설명10", dto.description());
            assertEquals(3, dto.participationCount());
            assertEquals(10, dto.maxParticipants());
            assertEquals(10_000, dto.dailyLimit());
            assertNull(dto.imageUrl());
            assertEquals(Set.of(AgeGroup.TWENTIES), dto.ageGroups());
            assertTrue(dto.jobGroups().isEmpty());
            assertEquals(createdAt, dto.createdAt());
            assertEquals(new ExploreRoomHostDto(11L, "호스트11", null, AgeGroup.THIRTIES, JobGroup.EMPLOYEE), dto.host());
        }

        @Test
        void 비공개방은_isPrivate가_true이고_공개방은_false다() {
            stubUserExists();
            stubRooms(room(10L, "1234"), room(11L, null));
            when(chatRoomMembershipRepository.findHostsByChatRoomIds(anyCollection()))
                    .thenReturn(List.of(host(10L, 1L), host(11L, 2L)));
            when(chatRoomMembershipRepository.findBannedChatRoomIds(REQUESTER_ID)).thenReturn(List.of());

            List<ExploreRoomDto> result = chatApiService.exploreChatRooms(REQUESTER_ID, noCondition);

            assertTrue(result.get(0).isPrivate());
            assertFalse(result.get(1).isPrivate());
        }

        @Test
        void 강퇴된_방은_isBanned가_true이고_나머지는_false다() {
            stubUserExists();
            stubRooms(room(10L, null), room(11L, null));
            when(chatRoomMembershipRepository.findHostsByChatRoomIds(anyCollection()))
                    .thenReturn(List.of(host(10L, 1L), host(11L, 2L)));
            when(chatRoomMembershipRepository.findBannedChatRoomIds(REQUESTER_ID)).thenReturn(List.of(11L));

            List<ExploreRoomDto> result = chatApiService.exploreChatRooms(REQUESTER_ID, noCondition);

            assertFalse(result.get(0).isBanned());
            assertTrue(result.get(1).isBanned());
        }

        @Test
        void 정원이_찬_방과_이미_참여_중인_방도_걸러내지_않는다() {
            // 마감 표시는 프론트가, 중복 참여 거부는 join API가 담당한다
            stubUserExists();
            ChatRoom full = room(10L, null);
            full.setParticipationCount(10);
            stubRooms(full, room(11L, null));
            when(chatRoomMembershipRepository.findHostsByChatRoomIds(anyCollection()))
                    .thenReturn(List.of(host(10L, 1L), host(11L, REQUESTER_ID)));
            when(chatRoomMembershipRepository.findBannedChatRoomIds(REQUESTER_ID)).thenReturn(List.of());

            List<ExploreRoomDto> result = chatApiService.exploreChatRooms(REQUESTER_ID, noCondition);

            assertEquals(2, result.size());
            assertEquals(10, result.get(0).participationCount());
        }

        @Test
        void 호스트_멤버십이_없는_방은_host가_null인_채로_남는다() {
            stubUserExists();
            stubRooms(room(10L, null));
            when(chatRoomMembershipRepository.findHostsByChatRoomIds(anyCollection())).thenReturn(List.of());
            when(chatRoomMembershipRepository.findBannedChatRoomIds(REQUESTER_ID)).thenReturn(List.of());

            List<ExploreRoomDto> result = chatApiService.exploreChatRooms(REQUESTER_ID, noCondition);

            assertEquals(1, result.size());
            assertNull(result.get(0).host());
        }

        @Test
        void 리포지토리가_돌려준_순서를_유지한다() {
            stubUserExists();
            stubRooms(room(30L, null), room(20L, null), room(10L, null));
            when(chatRoomMembershipRepository.findHostsByChatRoomIds(anyCollection())).thenReturn(List.of());
            when(chatRoomMembershipRepository.findBannedChatRoomIds(REQUESTER_ID)).thenReturn(List.of());

            List<Long> ids = chatApiService.exploreChatRooms(REQUESTER_ID, noCondition).stream()
                    .map(ExploreRoomDto::chatRoomId).toList();

            assertEquals(List.of(30L, 20L, 10L), ids);
        }

        @Test
        void 호스트_조회는_결과_방_id_목록으로_한_번만_한다() {
            stubUserExists();
            stubRooms(room(10L, null), room(11L, null));
            when(chatRoomMembershipRepository.findHostsByChatRoomIds(anyCollection())).thenReturn(List.of());
            when(chatRoomMembershipRepository.findBannedChatRoomIds(REQUESTER_ID)).thenReturn(List.of());

            chatApiService.exploreChatRooms(REQUESTER_ID, noCondition);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(chatRoomMembershipRepository).findHostsByChatRoomIds(captor.capture());
            assertEquals(Set.of(10L, 11L), Set.copyOf(captor.getValue()));
        }
    }
}
