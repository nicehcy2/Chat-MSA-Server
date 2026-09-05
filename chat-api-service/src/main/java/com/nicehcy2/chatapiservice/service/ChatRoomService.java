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
}
