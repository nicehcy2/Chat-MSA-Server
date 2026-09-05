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
import com.nicehcy2.chatapiservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .build());

        return room.getId();
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
