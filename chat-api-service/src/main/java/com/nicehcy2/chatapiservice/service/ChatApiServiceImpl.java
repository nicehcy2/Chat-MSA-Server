package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.dto.ChatRoomDetailDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomHostDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomInfoResponseDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomLastMessageDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomParticipantDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomUnreadCountDto;
import com.nicehcy2.chatapiservice.dto.ExploreChatRoomRequestDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomResponseDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomHostDto;
import com.nicehcy2.chatapiservice.dto.MembershipStatus;
import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.ChatRoomMembership;
import com.nicehcy2.chatapiservice.repository.ChatRoomMembershipRepository;
import com.nicehcy2.chatapiservice.repository.ChatRoomRepository;
import com.nicehcy2.chatapiservice.repository.MessageRepository;
import com.nicehcy2.chatapiservice.repository.UserRepository;
import com.nicehcy2.chatapiservice.common.error.GeneralException;
import com.nicehcy2.chatapiservice.common.error.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatApiServiceImpl implements ChatApiService {

    @Value("${CHAT_MESSAGE_PAGE_MAX_SIZE:100}") private int maxMessagePageSize;
    @Value("${CHAT_EXPLORE_PAGE_MAX_SIZE:50}") private int maxExplorePageSize;

    private static final int DEFAULT_EXPLORE_PAGE_SIZE = 20;

    private final MessageRepository messageRepository;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    /**
     * 참여 중인 채팅방 목록. 방 수와 무관하게 쿼리 3개(멤버십+방, 방별 unread, 방별 마지막 메시지)로 조립한다.
     */
    @Override
    public List<ChatRoomInfoResponseDto> getChatRoomDetails(Long userId) {

        List<ChatRoomMembership> memberships = chatRoomMembershipRepository.findActiveMembershipsWithChatRoom(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> unreadCounts = chatRoomMembershipRepository.countUnreadByUserId(userId).stream()
                .collect(Collectors.toMap(ChatRoomUnreadCountDto::chatRoomId, ChatRoomUnreadCountDto::count));

        List<Long> chatRoomIds = memberships.stream().map(cm -> cm.getChatRoom().getId()).toList();
        Map<Long, ChatRoomLastMessageDto> lastMessages = messageRepository.findLastMessages(chatRoomIds).stream()
                .collect(Collectors.toMap(ChatRoomLastMessageDto::chatRoomId, Function.identity()));

        Comparator<ChatRoom> latestFirst = Comparator
                .comparing((ChatRoom room) -> lastMessageId(lastMessages, room), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ChatRoom::getId, Comparator.reverseOrder());

        return memberships.stream()
                .map(ChatRoomMembership::getChatRoom)
                .sorted(latestFirst)
                .map(room -> toChatRoomInfo(room, unreadCounts.getOrDefault(room.getId(), 0L), lastMessages.get(room.getId())))
                .toList();
    }

    private static Long lastMessageId(Map<Long, ChatRoomLastMessageDto> lastMessages, ChatRoom room) {

        ChatRoomLastMessageDto lastMessage = lastMessages.get(room.getId());
        return lastMessage == null ? null : lastMessage.messageId();
    }

    private static ChatRoomInfoResponseDto toChatRoomInfo(ChatRoom room, long unreadCount, ChatRoomLastMessageDto lastMessage) {

        return ChatRoomInfoResponseDto.builder()
                .chatRoomId(room.getId())
                .chatRoomTitle(room.getTitle())
                .chatRoomMaxUserCount(room.getMaxParticipants())
                .chatRoomRule(room.getDescription())
                .chatRoomThumbnail(room.getImageUrl())
                .participationCount(room.getParticipationCount())
                .lastChatMessage(toPreview(lastMessage))
                .unreadChatCount((int) unreadCount)
                .updatedAt(lastMessage == null ? room.getUpdatedAt() : lastMessage.timestamp())
                .build();
    }

    private static String toPreview(ChatRoomLastMessageDto lastMessage) {

        if (lastMessage == null) {
            return null;
        }
        return switch (lastMessage.messageType()) {
            case TEXT -> lastMessage.content();
            case IMAGE -> "사진";
            case RECEIPT -> "영수증";
        };
    }

    @Override
    public List<MessageDto> getChatMessages(Long chatRoomId) {

        return messageRepository.findCustomByChatRoomId(chatRoomId);
    }

    @Override
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
    @Override
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

    @Override
    public List<ExploreRoomResponseDto> exploreChatRooms(Long requesterId, ExploreChatRoomRequestDto request) {

        if (!userRepository.existsById(requesterId)) {
            throw new GeneralException(ResponseCode.USER_NOT_FOUND);
        }

        int limit = request.limit() == null ? DEFAULT_EXPLORE_PAGE_SIZE : request.limit();
        if (limit < 1 || limit > maxExplorePageSize) {
            throw new IllegalArgumentException("limit은 1~" + maxExplorePageSize + " 범위여야 합니다.");
        }

        List<ChatRoom> rooms = chatRoomRepository.findForExplore(
                normalizeSearchKeyword(request.q()), request.ageGroup(), request.jobGroup(),
                request.before(), PageRequest.of(0, limit));
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Long> chatRoomIds = rooms.stream().map(ChatRoom::getId).toList();
        Map<Long, ChatRoomHostDto> hosts = chatRoomMembershipRepository.findHostsByChatRoomIds(chatRoomIds).stream()
                .collect(Collectors.toMap(ChatRoomHostDto::chatRoomId, Function.identity()));
        Set<Long> bannedIds = Set.copyOf(chatRoomMembershipRepository.findBannedChatRoomIds(requesterId));

        return rooms.stream()
                .map(room -> ExploreRoomResponseDto.from(
                        room, toHostDto(hosts.get(room.getId())), bannedIds.contains(room.getId())))
                .toList();
    }

    // 쿼리의 ESCAPE '!'와 짝. 이스케이프 문자 자체를 먼저 치환해야 뒤에 붙인 '!'가 다시 치환되지 않는다
    private static String normalizeSearchKeyword(String q) {

        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private static ExploreRoomHostDto toHostDto(ChatRoomHostDto host) {

        if (host == null) {
            return null;
        }
        return new ExploreRoomHostDto(host.userId(), host.nickname(), host.imageUrl(), host.ageGroup(), host.jobGroup());
    }

    @Override
    public ChatRoomDetailDto getChatRoomDetail(Long chatRoomId, Long requesterId) {

        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ResponseCode.CHATROOM_NOT_FOUND));

        MembershipStatus membershipStatus = chatRoomMembershipRepository.findByChatRoomIdAndUserId(chatRoomId, requesterId)
                .map(MembershipStatus::from)
                .orElse(MembershipStatus.NONE);

        ChatRoomHostDto host = chatRoomMembershipRepository.findHostsByChatRoomIds(List.of(chatRoomId)).stream()
                .findFirst()
                .orElse(null);

        return ChatRoomDetailDto.builder()
                .chatRoomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription())
                .participationCount(room.getParticipationCount())
                .maxParticipants(room.getMaxParticipants())
                .dailyLimit(room.getDailyLimit())
                .isPrivate(room.getPassword() != null)
                .imageUrl(room.getImageUrl())
                .ageGroups(room.getAgeGroups())
                .jobGroups(room.getJobGroups())
                .createdAt(room.getCreatedAt())
                .host(toHostDto(host))
                .membershipStatus(membershipStatus)
                .build();
    }
}
