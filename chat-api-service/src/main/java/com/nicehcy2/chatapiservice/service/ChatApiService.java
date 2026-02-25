package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.dto.ChatRoomInfoResponseDto;
import com.nicehcy2.chatapiservice.dto.ChatServerInfoResponse;
import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.User;
import com.nicehcy2.chatapiservice.repository.ChatRoomMembershipRepository;
import com.nicehcy2.chatapiservice.repository.MessageRepository;
import com.nicehcy2.chatapiservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatApiService {

    private final DiscoveryClient discoveryClient;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;

    public ChatServerInfoResponse assignChatServer() {

        // 적절한 채팅 서버 가져오기
        return getServerInstance();
    }

    /**
     * 구독한 채팅방 전체 조회
     * @param userId
     * @return
     */
    public List<ChatRoomInfoResponseDto> getChatRoomDetails(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 없습니다"));

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

    private ChatServerInfoResponse getServerInstance() {

        List<ServiceInstance> instances = discoveryClient.getInstances("chat-service");

        if (instances.size() == 0) {
            return null;
        }
        // TODO: get(0) 로직 말고 서버를 선택하는 조건을 추가해줘야 한다.
        String nodeId = instances.get(0).getInstanceId();
        String serviceUri = String.valueOf(instances.get(0).getUri());

        return ChatServerInfoResponse.builder()
                .nodeId(nodeId)
                .websocketUrl(serviceUri)
                .build();
    }

    public List<MessageDto> getChatMessages(Long chatRoomId) {

        return messageRepository.findCustomByChatRoomId(chatRoomId);
    }
}
