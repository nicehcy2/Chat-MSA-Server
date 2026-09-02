package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.dto.ChatRoomInfoResponseDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomParticipantDto;
import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.ChatRoomMembership;
import com.nicehcy2.chatapiservice.entity.User;
import com.nicehcy2.chatapiservice.repository.ChatRoomMembershipRepository;
import com.nicehcy2.chatapiservice.repository.MessageRepository;
import com.nicehcy2.chatapiservice.repository.UserRepository;
import com.nicehcy2.chatapiservice.common.error.GeneralException;
import com.nicehcy2.chatapiservice.common.error.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatApiService {

    @Value("${CHAT_MESSAGE_PAGE_MAX_SIZE:100}") private int maxMessagePageSize;

    private final DiscoveryClient discoveryClient;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;

    /**
     * 구독한 채팅방 전체 조회
     * @param userId
     * @return
     */
    public List<ChatRoomInfoResponseDto> getChatRoomDetails(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));

        List<ChatRoom> chatRooms = chatRoomMembershipRepository.findChatRoomByUserId(user.getUserId());

        return chatRooms.stream()
                .map(chatRoom -> {
                    return ChatRoomInfoResponseDto.builder()
                            .chatRoomId(chatRoom.getId())
                            .chatRoomTitle(chatRoom.getTitle())
                            .chatRoomMaxUserCount(chatRoom.getMaxParticipants())
                            .chatRoomRule(chatRoom.getDescription())
                            .chatRoomThumbnail(chatRoom.getImageUrl())
                            .participationCount(chatRoom.getParticipationCount())
                            .lastChatMessage("LAST")
                            .unreadChatCount(13)
                            .updatedAt(chatRoom.getUpdatedAt())
                            .build();
                }).toList();
    }

    public List<MessageDto> getChatMessages(Long chatRoomId) {

        return messageRepository.findCustomByChatRoomId(chatRoomId);
    }

    public List<MessageDto> getChatMessagesBefore(Long chatRoomId, Long requesterId, Long before, int limit) {

        if (limit < 1 || limit > maxMessagePageSize) {
            throw new IllegalArgumentException("limit은 1~" + maxMessagePageSize + " 범위여야 합니다.");
        }

        ChatRoomMembership membership = chatRoomMembershipRepository
                .findByChatRoomIdAndUserIdAndLeftAtIsNullAndIsBannedFalse(chatRoomId, requesterId)
                .orElseThrow(() -> new GeneralException(ResponseCode.CHATROOM_ACCESS_DENIED));

        List<MessageDto> messages = new ArrayList<>(messageRepository.findMessagesBefore(
                chatRoomId, before, membership.getJoinMessageId(), PageRequest.of(0, limit)));
        Collections.reverse(messages);
        return messages;
    }

    /**
     * 방 참여자 + 읽음 워터마크 스냅샷.
     * 클라이언트는 방 입장 시 이 스냅샷으로 참여자 Map을 만들고,
     * 이후에는 /sub/chatroom{id}.read 델타를 max-병합하여 unread를 계산한다.
     *
     * 요청자가 해당 방의 활성 멤버가 아니면 403. 별도 exists 쿼리 대신
     * 어차피 조회하는 참여자 목록에 요청자가 포함되는지로 검증한다(쿼리 1회).
     */
    public List<ChatRoomParticipantDto> getChatRoomParticipants(Long chatRoomId, Long requesterId) {

        List<ChatRoomParticipantDto> participants =
                chatRoomMembershipRepository.findParticipantsByChatRoomId(chatRoomId);

        boolean isMember = participants.stream()
                .anyMatch(participant -> participant.userId().equals(requesterId));
        if (!isMember) {
            throw new GeneralException(ResponseCode.CHATROOM_ACCESS_DENIED);
        }

        return participants;
    }
}
