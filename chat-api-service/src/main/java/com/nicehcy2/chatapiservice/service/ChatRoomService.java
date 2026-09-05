package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.dto.CreateChatRoomRequestDto;

/**
 * 방 생명주기(생성, 이후 참여/퇴장/강퇴). 조회는 {@link ChatApiService}.
 */
public interface ChatRoomService {

    /**
     * 방을 생성하고 요청자를 호스트 멤버십으로 등록한다.
     * @return 생성된 chatRoomId
     */
    Long createChatRoom(Long requesterId, CreateChatRoomRequestDto dto);

    /**
     * 방에 참여한다. 이미 활성 멤버면 CHATROOM_ALREADY_JOINED, 나갔던 유저는 기존 행을 재활성화한다.
     * @param password 비공개방일 때만 검사. 공개방은 무시
     * @return chatRoomId
     */
    Long joinChatRoom(Long requesterId, Long chatRoomId, String password);
}
