package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.dto.ChatRoomDetailDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomInfoResponseDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomParticipantDto;
import com.nicehcy2.chatapiservice.dto.ExploreChatRoomRequestDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomResponseDto;
import com.nicehcy2.chatapiservice.dto.MessageDto;

import java.util.List;

/**
 * 채팅 조회 API. 방 생명주기(생성/참여/퇴장)는 {@link ChatRoomService}.
 */
public interface ChatApiService {

    /** 참여 중인 채팅방 목록 (안 읽은 수, 마지막 메시지 포함) */
    List<ChatRoomInfoResponseDto> getChatRoomDetails(Long userId);

    List<MessageDto> getChatMessages(Long chatRoomId);

    /**
     * 커서 기반 메시지 동기화. before 이전(exclusive) 메시지를 시간순(ASC)으로 최대 limit개.
     * 요청자가 활성 멤버가 아니면 CHATROOM_ACCESS_DENIED.
     */
    List<MessageDto> getChatMessagesBefore(Long chatRoomId, Long requesterId, Long before, int limit);

    /** 방 참여자 + 읽음 워터마크 스냅샷. 요청자가 활성 멤버가 아니면 CHATROOM_ACCESS_DENIED. */
    List<ChatRoomParticipantDto> getChatRoomParticipants(Long chatRoomId, Long requesterId);

    List<ExploreRoomResponseDto> exploreChatRooms(Long requesterId, ExploreChatRoomRequestDto request);

    ChatRoomDetailDto getChatRoomDetail(Long chatRoomId, Long requesterId);
}
