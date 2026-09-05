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
import com.nicehcy2.chatapiservice.repository.MessageRepository;
import com.nicehcy2.chatapiservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRoomMembershipRepository chatRoomMembershipRepository;
    @Mock UserRepository userRepository;
    @Mock MessageRepository messageRepository;

    @InjectMocks ChatRoomServiceImpl chatRoomService;

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

    // =====================================================================
    // POST /api/chats/{id}/join
    // 검사 순서: 유저 존재 → 방 존재 → 멤버십 상태(강퇴/활성/나감) → 비밀번호 → 정원(조건부 UPDATE) → 멤버십 저장
    // =====================================================================
    @Nested
    class 참여 {

        static final Long ROOM_ID = 10L;
        static final Long LATEST_MESSAGE_ID = 500L;

        ChatRoom publicRoomEntity() {
            return ChatRoom.builder()
                    .id(ROOM_ID).title("무지출 챌린지")
                    .maxParticipants(10).participationCount(3)
                    .password(null)
                    .build();
        }

        ChatRoom privateRoomEntity() {
            return ChatRoom.builder()
                    .id(ROOM_ID).title("비공개 방")
                    .maxParticipants(10).participationCount(3)
                    .password("1234")
                    .build();
        }

        ChatRoomMembership membership(ChatRoom room, boolean banned, LocalDateTime leftAt) {
            return ChatRoomMembership.builder()
                    .userId(REQUESTER_ID).chatRoom(room)
                    .isHost(false).isBanned(banned).leftAt(leftAt)
                    .joinedAt(LocalDateTime.now().minusDays(10))
                    .joinMessageId(100L)
                    .build();
        }

        void stubUserAndRoom(ChatRoom room) {
            when(userRepository.existsById(REQUESTER_ID)).thenReturn(true);
            when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        }

        void stubMembership(Optional<ChatRoomMembership> membership) {
            when(chatRoomMembershipRepository.findByChatRoomIdAndUserId(ROOM_ID, REQUESTER_ID)).thenReturn(membership);
        }

        /** 자리 확보 성공 + 최신 메시지 존재 + 저장 echo. 신규 참여 성공 경로 공통 stub. */
        void stubJoinSucceeds() {
            when(chatRoomRepository.incrementParticipationCountIfNotFull(ROOM_ID)).thenReturn(1);
            when(messageRepository.findMaxIdByChatRoomId(ROOM_ID)).thenReturn(Optional.of(LATEST_MESSAGE_ID));
            when(chatRoomMembershipRepository.save(any(ChatRoomMembership.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        ChatRoomMembership capturedSavedMembership() {
            ArgumentCaptor<ChatRoomMembership> captor = ArgumentCaptor.forClass(ChatRoomMembership.class);
            verify(chatRoomMembershipRepository).save(captor.capture());
            return captor.getValue();
        }

        @Nested
        class 정상 {

            @Test
            void 공개방_참여하면_일반_멤버십이_저장되고_자리를_확보하고_방_id를_반환한다() {
                ChatRoom room = publicRoomEntity();
                stubUserAndRoom(room);
                stubMembership(Optional.empty());
                stubJoinSucceeds();

                Long result = chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null);

                assertEquals(ROOM_ID, result);
                verify(chatRoomRepository).incrementParticipationCountIfNotFull(ROOM_ID);

                ChatRoomMembership saved = capturedSavedMembership();
                assertEquals(REQUESTER_ID, saved.getUserId());
                assertSame(room, saved.getChatRoom());
                assertFalse(saved.getIsHost());
                assertFalse(saved.getIsBanned());
                assertNull(saved.getLeftAt());
            }

            @Test
            void 비공개방은_비밀번호가_맞으면_참여된다() {
                stubUserAndRoom(privateRoomEntity());
                stubMembership(Optional.empty());
                stubJoinSucceeds();

                Long result = chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, "1234");

                assertEquals(ROOM_ID, result);
                verify(chatRoomMembershipRepository).save(any(ChatRoomMembership.class));
            }

            @Test
            void 공개방인데_비밀번호가_딸려오면_무시하고_참여된다() {
                stubUserAndRoom(publicRoomEntity());
                stubMembership(Optional.empty());
                stubJoinSucceeds();

                Long result = chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, "9999");

                assertEquals(ROOM_ID, result);
                verify(chatRoomMembershipRepository).save(any(ChatRoomMembership.class));
            }

            @Test
            void joinMessageId는_참여_시점_방의_최신_메시지_id다() {
                // 히스토리 floor: 이 값 이하의 메시지는 커서 조회에서 제외되어 참여 전 대화가 보이지 않는다
                stubUserAndRoom(publicRoomEntity());
                stubMembership(Optional.empty());
                stubJoinSucceeds();

                chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null);

                assertEquals(LATEST_MESSAGE_ID, capturedSavedMembership().getJoinMessageId());
            }

            @Test
            void 메시지가_없는_방에_참여하면_joinMessageId는_null이다() {
                stubUserAndRoom(publicRoomEntity());
                stubMembership(Optional.empty());
                when(chatRoomRepository.incrementParticipationCountIfNotFull(ROOM_ID)).thenReturn(1);
                when(messageRepository.findMaxIdByChatRoomId(ROOM_ID)).thenReturn(Optional.empty());
                when(chatRoomMembershipRepository.save(any(ChatRoomMembership.class))).thenAnswer(inv -> inv.getArgument(0));

                chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null);

                assertNull(capturedSavedMembership().getJoinMessageId());
            }

            @Test
            void 나갔던_유저가_재참여하면_기존_행이_재활성화되고_새_행은_만들지_않는다() {
                // (방, 유저)당 1행 모델. 나가 있던 동안의 대화는 보이지 않도록 floor를 새로 잡는다
                ChatRoom room = publicRoomEntity();
                ChatRoomMembership left = membership(room, false, LocalDateTime.now().minusDays(1));
                LocalDateTime previousJoinedAt = left.getJoinedAt();
                stubUserAndRoom(room);
                stubMembership(Optional.of(left));
                when(chatRoomRepository.incrementParticipationCountIfNotFull(ROOM_ID)).thenReturn(1);
                when(messageRepository.findMaxIdByChatRoomId(ROOM_ID)).thenReturn(Optional.of(LATEST_MESSAGE_ID));

                Long result = chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null);

                assertEquals(ROOM_ID, result);
                // 기존 행이 재활성화됨
                assertNull(left.getLeftAt());
                assertEquals(LATEST_MESSAGE_ID, left.getJoinMessageId());
                assertTrue(left.getJoinedAt().isAfter(previousJoinedAt));
                assertFalse(left.getIsBanned());
                // 자리는 다시 확보해야 한다 (나갈 때 count가 줄었으므로)
                verify(chatRoomRepository).incrementParticipationCountIfNotFull(ROOM_ID);
                // save가 불리더라도 새 객체가 아닌 기존 행이어야 한다
                ArgumentCaptor<ChatRoomMembership> captor = ArgumentCaptor.forClass(ChatRoomMembership.class);
                verify(chatRoomMembershipRepository, atMost(1)).save(captor.capture());
                captor.getAllValues().forEach(saved -> assertSame(left, saved));
            }

            @Test
            void 자리_확보_UPDATE가_멤버십_저장보다_먼저_실행된다() {
                // 같은 유저의 동시 join: 두 번째 INSERT가 unique에 걸려 트랜잭션이 롤백될 때
                // 먼저 실행된 UPDATE도 함께 취소되어 count가 어긋나지 않는다. 이 순서가 그 전제.
                stubUserAndRoom(publicRoomEntity());
                stubMembership(Optional.empty());
                stubJoinSucceeds();

                chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null);

                InOrder order = inOrder(chatRoomRepository, chatRoomMembershipRepository);
                order.verify(chatRoomRepository).incrementParticipationCountIfNotFull(ROOM_ID);
                order.verify(chatRoomMembershipRepository).save(any(ChatRoomMembership.class));
            }
        }

        @Nested
        class 거부 {

            @Test
            void 방이_없으면_404() {
                when(userRepository.existsById(REQUESTER_ID)).thenReturn(true);
                when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null));

                assertEquals(ResponseCode.CHATROOM_NOT_FOUND, e.getErrorCode());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 존재하지_않는_유저면_404() {
                when(userRepository.existsById(REQUESTER_ID)).thenReturn(false);

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null));

                assertEquals(ResponseCode.USER_NOT_FOUND, e.getErrorCode());
                verify(chatRoomRepository, never()).incrementParticipationCountIfNotFull(any());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 이미_활성_멤버면_409이고_아무것도_바꾸지_않는다() {
                ChatRoom room = publicRoomEntity();
                stubUserAndRoom(room);
                stubMembership(Optional.of(membership(room, false, null)));

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null));

                assertEquals(ResponseCode.CHATROOM_ALREADY_JOINED, e.getErrorCode());
                verify(chatRoomRepository, never()).incrementParticipationCountIfNotFull(any());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 이미_활성_멤버면_비밀번호_검사보다_먼저_거부된다() {
                // 멤버십 상태 판정이 비밀번호보다 앞선다 (강퇴와 같은 위치)
                ChatRoom room = privateRoomEntity();
                stubUserAndRoom(room);
                stubMembership(Optional.of(membership(room, false, null)));

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, "0000"));

                assertEquals(ResponseCode.CHATROOM_ALREADY_JOINED, e.getErrorCode());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 정원이_꽉_찼으면_409이고_멤버십을_만들지_않는다() {
                // 조건부 UPDATE(WHERE participation_count < max_participants)의 영향 행이 0 = 자리 없음.
                // 읽고-비교하고-쓰는 대신 UPDATE 한 문장으로 판정해 마지막 1자리 동시 요청 경합을 막는다.
                stubUserAndRoom(publicRoomEntity());
                stubMembership(Optional.empty());
                when(chatRoomRepository.incrementParticipationCountIfNotFull(ROOM_ID)).thenReturn(0);

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null));

                assertEquals(ResponseCode.CHATROOM_FULL, e.getErrorCode());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 비공개방에_비밀번호_없이_오면_403() {
                stubUserAndRoom(privateRoomEntity());
                stubMembership(Optional.empty());

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null));

                assertEquals(ResponseCode.CHATROOM_PASSWORD_MISMATCH, e.getErrorCode());
                verify(chatRoomRepository, never()).incrementParticipationCountIfNotFull(any());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 비공개방에_틀린_비밀번호면_403() {
                stubUserAndRoom(privateRoomEntity());
                stubMembership(Optional.empty());

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, "0000"));

                assertEquals(ResponseCode.CHATROOM_PASSWORD_MISMATCH, e.getErrorCode());
                verify(chatRoomRepository, never()).incrementParticipationCountIfNotFull(any());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 강퇴_이력이_있으면_403이고_재활성화되지_않는다() {
                ChatRoom room = publicRoomEntity();
                ChatRoomMembership banned = membership(room, true, LocalDateTime.now().minusDays(1));
                stubUserAndRoom(room);
                stubMembership(Optional.of(banned));

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null));

                assertEquals(ResponseCode.CHATROOM_BANNED, e.getErrorCode());
                assertTrue(banned.getIsBanned());
                assertNotNull(banned.getLeftAt());
                verify(chatRoomRepository, never()).incrementParticipationCountIfNotFull(any());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 나갔던_유저_재참여도_정원이_꽉_찼으면_409이고_기존_행을_건드리지_않는다() {
                // 자리 확보 UPDATE가 실패한 뒤에 엔티티를 바꾸면 안 된다 (신규 경로와 같은 순서: 자리 확보 → 변경).
                // 예외로 롤백되니 DB는 안전하지만, 같은 트랜잭션 안에서 이 객체를 읽는 코드가 잘못된 상태를 보게 된다.
                ChatRoom room = publicRoomEntity();
                ChatRoomMembership left = membership(room, false, LocalDateTime.now().minusDays(1));
                LocalDateTime previousLeftAt = left.getLeftAt();
                Long previousFloor = left.getJoinMessageId();
                stubUserAndRoom(room);
                stubMembership(Optional.of(left));
                when(chatRoomRepository.incrementParticipationCountIfNotFull(ROOM_ID)).thenReturn(0);

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, null));

                assertEquals(ResponseCode.CHATROOM_FULL, e.getErrorCode());
                assertEquals(previousLeftAt, left.getLeftAt());
                assertEquals(previousFloor, left.getJoinMessageId());
                verify(chatRoomMembershipRepository, never()).save(any());
            }

            @Test
            void 강퇴_이력이_있으면_비밀번호가_틀려도_강퇴_에러가_먼저다() {
                ChatRoom room = privateRoomEntity();
                stubUserAndRoom(room);
                stubMembership(Optional.of(membership(room, true, LocalDateTime.now().minusDays(1))));

                GeneralException e = assertThrows(GeneralException.class,
                        () -> chatRoomService.joinChatRoom(REQUESTER_ID, ROOM_ID, "0000"));

                assertEquals(ResponseCode.CHATROOM_BANNED, e.getErrorCode());
            }
        }
    }
}
