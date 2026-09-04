package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.common.error.GeneralException;
import com.nicehcy2.chatapiservice.common.error.ResponseCode;
import com.nicehcy2.chatapiservice.dto.CreateChatRoomRequestDto;
import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.ChatRoomMembership;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import com.nicehcy2.chatapiservice.repository.ChatRoomMembershipRepository;
import com.nicehcy2.chatapiservice.repository.ChatRoomRepository;
import com.nicehcy2.chatapiservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRoomMembershipRepository chatRoomMembershipRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ChatRoomService chatRoomService;

    static final Long REQUESTER_ID = 7L;
    static final Long SAVED_ROOM_ID = 10L;

    /** 공개방 기본 요청. 케이스별로 toBuilder()로 필요한 필드만 바꿔 쓴다. */
    CreateChatRoomRequestDto publicRoom;

    @BeforeEach
    void setUp() {
        publicRoom = CreateChatRoomRequestDto.builder()
                .title("무지출 챌린지")
                .description("하루 만원으로 살기")
                .maxParticipants(10)
                .isPrivate(false)
                .password(null)
                .ageGroups(List.of(AgeGroup.TWENTIES))
                .jobGroups(List.of(JobGroup.EMPLOYEE))
                .dailyLimit(10_000)
                .imageUrl(null)
                .build();
    }

    /** 유저 존재 + 저장 시 id 부여. 성공 경로 공통 stub. */
    void stubHappyPath() {
        when(userRepository.existsById(REQUESTER_ID)).thenReturn(true);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(inv -> {
            ChatRoom room = inv.getArgument(0);
            room.setId(SAVED_ROOM_ID);
            return room;
        });
        when(chatRoomMembershipRepository.save(any(ChatRoomMembership.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    class 정상_생성 {

        @Test
        void 공개방_생성하면_방과_호스트_멤버십이_저장되고_방_id를_반환한다() {
            stubHappyPath();

            Long result = chatRoomService.createChatRoom(REQUESTER_ID, publicRoom);

            assertEquals(SAVED_ROOM_ID, result);

            // 방: 요청값 반영 + 참여 인원 1(호스트) + 공개방이라 비밀번호 없음
            ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(roomCaptor.capture());
            ChatRoom room = roomCaptor.getValue();
            assertEquals("무지출 챌린지", room.getTitle());
            assertEquals("하루 만원으로 살기", room.getDescription());
            assertEquals(10, room.getMaxParticipants());
            assertEquals(1, room.getParticipationCount());
            assertEquals(10_000, room.getDailyLimit());
            assertNull(room.getPassword());
            assertNull(room.getImageUrl());
            assertEquals(Set.of(AgeGroup.TWENTIES), room.getAgeGroups());
            assertEquals(Set.of(JobGroup.EMPLOYEE), room.getJobGroups());

            // 멤버십: 생성자가 호스트, 활성 상태, 히스토리 floor 없음(방 생성 시점엔 메시지가 없으므로)
            ArgumentCaptor<ChatRoomMembership> membershipCaptor = ArgumentCaptor.forClass(ChatRoomMembership.class);
            verify(chatRoomMembershipRepository).save(membershipCaptor.capture());
            ChatRoomMembership membership = membershipCaptor.getValue();
            assertEquals(REQUESTER_ID, membership.getUserId());
            assertSame(room, membership.getChatRoom());
            assertTrue(membership.getIsHost());
            assertFalse(membership.getIsBanned());
            assertNull(membership.getLeftAt());
            assertNull(membership.getJoinMessageId());
        }

        @Test
        void 비공개방은_비밀번호를_평문_그대로_저장한다() {
            stubHappyPath();
            CreateChatRoomRequestDto privateRoom = publicRoom.toBuilder()
                    .isPrivate(true).password("1234").build();

            chatRoomService.createChatRoom(REQUESTER_ID, privateRoom);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertEquals("1234", captor.getValue().getPassword());
        }

        @Test
        void 공개방인데_비밀번호가_딸려오면_무시하고_null로_저장한다() {
            stubHappyPath();
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .isPrivate(false).password("1234").build();

            chatRoomService.createChatRoom(REQUESTER_ID, request);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertNull(captor.getValue().getPassword());
        }

        @Test
        void 대상_그룹이_비어있으면_전체_대상_의미로_빈_Set을_저장한다() {
            stubHappyPath();
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .ageGroups(List.of()).jobGroups(List.of()).build();

            chatRoomService.createChatRoom(REQUESTER_ID, request);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertTrue(captor.getValue().getAgeGroups().isEmpty());
            assertTrue(captor.getValue().getJobGroups().isEmpty());
        }

        @Test
        void 대상_그룹이_null이어도_빈_Set으로_저장한다() {
            stubHappyPath();
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .ageGroups(null).jobGroups(null).build();

            chatRoomService.createChatRoom(REQUESTER_ID, request);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertNotNull(captor.getValue().getAgeGroups());
            assertTrue(captor.getValue().getAgeGroups().isEmpty());
            assertNotNull(captor.getValue().getJobGroups());
            assertTrue(captor.getValue().getJobGroups().isEmpty());
        }

        @Test
        void 대상_그룹_중복은_제거되고_여러_값은_모두_유지된다() {
            stubHappyPath();
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .ageGroups(List.of(AgeGroup.TWENTIES, AgeGroup.TWENTIES, AgeGroup.THIRTIES))
                    .jobGroups(List.of(JobGroup.STUDENT, JobGroup.EMPLOYEE, JobGroup.STUDENT))
                    .build();

            chatRoomService.createChatRoom(REQUESTER_ID, request);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertEquals(Set.of(AgeGroup.TWENTIES, AgeGroup.THIRTIES), captor.getValue().getAgeGroups());
            assertEquals(Set.of(JobGroup.STUDENT, JobGroup.EMPLOYEE), captor.getValue().getJobGroups());
        }

        @Test
        void 대상_그룹의_null_요소는_제거한다() {
            // Jackson은 리스트 안의 null을 통과시킨다. 그대로 @ElementCollection에 들어가면 INSERT에서 DB 에러(500)
            stubHappyPath();
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .ageGroups(Arrays.asList(null, AgeGroup.TWENTIES, null))
                    .jobGroups(Arrays.asList((JobGroup) null))
                    .build();

            chatRoomService.createChatRoom(REQUESTER_ID, request);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertEquals(Set.of(AgeGroup.TWENTIES), captor.getValue().getAgeGroups());
            assertTrue(captor.getValue().getJobGroups().isEmpty());
        }

        @Test
        void title과_description의_앞뒤_공백은_잘라서_저장한다() {
            // 프론트가 trim해서 보내지만 서버도 방어한다. (길이 검증은 raw 기준 → DTO 테스트 참고)
            stubHappyPath();
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .title("  무지출 챌린지  ")
                    .description("  하루 만원  ")
                    .build();

            chatRoomService.createChatRoom(REQUESTER_ID, request);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertEquals("무지출 챌린지", captor.getValue().getTitle());
            assertEquals("하루 만원", captor.getValue().getDescription());
        }

        @Test
        void description이_비어있거나_공백만이면_null로_저장한다() {
            stubHappyPath();

            chatRoomService.createChatRoom(REQUESTER_ID, publicRoom.toBuilder().description("   ").build());
            chatRoomService.createChatRoom(REQUESTER_ID, publicRoom.toBuilder().description("").build());
            chatRoomService.createChatRoom(REQUESTER_ID, publicRoom.toBuilder().description(null).build());

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository, times(3)).save(captor.capture());
            captor.getAllValues().forEach(room -> assertNull(room.getDescription()));
        }

        @Test
        void UNDECIDED는_정식_대상값이므로_제거하지_않는다() {
            // "나이를 밝히지 않은 사람들을 위한 방"은 기획에 있는 케이스
            stubHappyPath();
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .ageGroups(List.of(AgeGroup.UNDECIDED, AgeGroup.TWENTIES))
                    .jobGroups(List.of(JobGroup.UNDECIDED))
                    .build();

            chatRoomService.createChatRoom(REQUESTER_ID, request);

            ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
            verify(chatRoomRepository).save(captor.capture());
            assertEquals(Set.of(AgeGroup.UNDECIDED, AgeGroup.TWENTIES), captor.getValue().getAgeGroups());
            assertEquals(Set.of(JobGroup.UNDECIDED), captor.getValue().getJobGroups());
        }
    }

    @Nested
    class 생성_거부 {

        @Test
        void 비공개방인데_비밀번호가_없으면_400이고_fieldErrors에_password가_담긴다() {
            when(userRepository.existsById(REQUESTER_ID)).thenReturn(true);
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .isPrivate(true).password(null).build();

            GeneralException e = assertThrows(GeneralException.class,
                    () -> chatRoomService.createChatRoom(REQUESTER_ID, request));

            assertEquals(ResponseCode._BAD_REQUEST, e.getErrorCode());
            assertTrue(e.getFieldErrors().stream().anyMatch(f -> f.field().equals("password")));
            verify(chatRoomRepository, never()).save(any());
            verify(chatRoomMembershipRepository, never()).save(any());
        }

        @Test
        void 비공개방인데_비밀번호가_공백이면_없는_것으로_본다() {
            when(userRepository.existsById(REQUESTER_ID)).thenReturn(true);
            CreateChatRoomRequestDto request = publicRoom.toBuilder()
                    .isPrivate(true).password("   ").build();

            GeneralException e = assertThrows(GeneralException.class,
                    () -> chatRoomService.createChatRoom(REQUESTER_ID, request));

            assertEquals(ResponseCode._BAD_REQUEST, e.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        void 존재하지_않는_유저면_404이고_아무것도_저장하지_않는다() {
            // membership.userId가 FK가 아니라 DB가 막아주지 않는다.
            // TODO(탈퇴 API 구현 시): status=false(탈퇴) 유저도 거부하도록 findById + status 검사로 확장
            when(userRepository.existsById(REQUESTER_ID)).thenReturn(false);

            GeneralException e = assertThrows(GeneralException.class,
                    () -> chatRoomService.createChatRoom(REQUESTER_ID, publicRoom));

            assertEquals(ResponseCode.USER_NOT_FOUND, e.getErrorCode());
            verify(chatRoomRepository, never()).save(any());
            verify(chatRoomMembershipRepository, never()).save(any());
        }
    }
}
