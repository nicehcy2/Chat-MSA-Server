package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.common.error.FieldErrorDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 방 생명주기(생성, 이후 join/leave/kick). 조회는 ChatApiService.
 */
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Transactional
    @Override
    public Long createChatRoom(Long requesterId, CreateChatRoomRequestDto dto) {

        if (!userRepository.existsById(requesterId)) {
            throw new GeneralException(ResponseCode.USER_NOT_FOUND);
        }

        // 4자리 PIN이라 해시 가치가 낮아 평문 저장 (결정 사항)
        String password = null;
        if (dto.isPrivate()) {
            if (dto.password() == null || dto.password().isBlank()) {
                throw new GeneralException(ResponseCode._BAD_REQUEST,
                        List.of(new FieldErrorDto("password", "비공개 방은 비밀번호가 필요합니다.")));
            }
            password = dto.password();
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .title(dto.title().trim())
                .description(normalizeDescription(dto.description()))
                .password(password)
                .maxParticipants(dto.maxParticipants())
                .participationCount(1)
                .dailyLimit(dto.dailyLimit())
                .imageUrl(dto.imageUrl())
                .ageGroups(toEnumSet(dto.ageGroups(), AgeGroup.class))
                .jobGroups(toEnumSet(dto.jobGroups(), JobGroup.class))
                .build());

        chatRoomMembershipRepository.save(ChatRoomMembership.builder()
                .userId(requesterId)
                .chatRoom(room)
                .isHost(true)
                .isBanned(false)
                .joinedAt(LocalDateTime.now())
                .build());

        return room.getId();
    }

    // 자리 확보 UPDATE가 멤버십 INSERT/변경보다 먼저여야 한다. 같은 유저의 동시 join에서
    // 두 번째 INSERT가 unique에 걸려 롤백될 때 UPDATE도 함께 취소되어 count가 어긋나지 않는다.
    @Transactional
    @Override
    public Long joinChatRoom(Long requesterId, Long chatRoomId, String password) {

        if (!userRepository.existsById(requesterId)) {
            throw new GeneralException(ResponseCode.USER_NOT_FOUND);
        }
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ResponseCode.CHATROOM_NOT_FOUND));

        ChatRoomMembership existing = chatRoomMembershipRepository
                .findByChatRoomIdAndUserId(chatRoomId, requesterId)
                .orElse(null);

        if (existing != null) {
            if (existing.getIsBanned()) {
                throw new GeneralException(ResponseCode.CHATROOM_BANNED);
            }
            if (existing.getLeftAt() == null) {
                throw new GeneralException(ResponseCode.CHATROOM_ALREADY_JOINED);
            }
        }

        if (room.getPassword() != null && !room.getPassword().equals(password)) {
            throw new GeneralException(ResponseCode.CHATROOM_PASSWORD_MISMATCH);
        }

        if (chatRoomRepository.incrementParticipationCountIfNotFull(chatRoomId) == 0) {
            throw new GeneralException(ResponseCode.CHATROOM_FULL);
        }

        Long floor = messageRepository.findMaxIdByChatRoomId(chatRoomId).orElse(null);

        if (existing != null) {
            existing.rejoin(floor);
            return chatRoomId;
        }

        chatRoomMembershipRepository.save(ChatRoomMembership.builder()
                .userId(requesterId)
                .chatRoom(room)
                .isHost(false)
                .isBanned(false)
                .leftAt(null)
                .joinMessageId(floor)
                .joinedAt(LocalDateTime.now())
                .build());

        return chatRoomId;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    // Jackson은 리스트 안의 null을 통과시키므로 여기서 걸러야 @ElementCollection INSERT가 깨지지 않는다
    private static <E extends Enum<E>> Set<E> toEnumSet(List<E> values, Class<E> type) {
        Set<E> result = EnumSet.noneOf(type);
        if (values != null) {
            values.stream().filter(Objects::nonNull).forEach(result::add);
        }
        return result;
    }
}
