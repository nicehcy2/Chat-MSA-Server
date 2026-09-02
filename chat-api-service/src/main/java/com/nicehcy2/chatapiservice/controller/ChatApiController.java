package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.dto.ChatRoomInfoResponseDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomParticipantDto;
import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.service.ChatApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatApiController {

    private final ChatApiService chatApiService;

    /**
     * 커서 기반 메시지 동기화.
     * before(선택): 이 messageTSID 이전(exclusive) 메시지만. 생략 시 최신부터.
     * limit(기본 30, 최대 100): 페이지 크기.
     * 응답은 시간순(ASC). 다음 페이지 커서 = 첫 요소의 messageTSID, 끝 판정 = size < limit.
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<List<MessageDto>> getChatMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "30") int limit,
            @RequestHeader("X-User-Id") Long requesterId) {

        return ResponseEntity.ok(chatApiService.getChatMessagesBefore(chatRoomId, requesterId, before, limit));
    }

    // TODO(보안): userId를 쿼리 파라미터가 아니라 게이트웨이가 주입하는 X-User-Id 헤더로 전환할 것. 지금은 아무 userId나 넣어 남의 방 목록을 볼 수 있다.
    @GetMapping
    public ResponseEntity<List<ChatRoomInfoResponseDto>> getChatRoomList(@RequestParam Long userId) {

        return ResponseEntity.ok(chatApiService.getChatRoomDetails(userId));
    }

    /**
     * 방 참여자 + 읽음 워터마크 스냅샷 조회.
     * 요청자 신원은 게이트웨이가 JWT에서 추출해 주입하는 X-User-Id 헤더를 사용한다.
     */
    @GetMapping("/{chatRoomId}/participants")
    public ResponseEntity<List<ChatRoomParticipantDto>> getChatRoomParticipants(
            @PathVariable Long chatRoomId,
            @RequestHeader("X-User-Id") Long requesterId) {

        return ResponseEntity.ok(chatApiService.getChatRoomParticipants(chatRoomId, requesterId));
    }

    /**
     * 채팅방 만들기
     * @return
     */
    /*
    @PostMapping
    public ResponseEntity<> createChatRoom() {

    }*/

    /**
     * 특정 채팅방 조회
     */
    /*
    @GetMapping("/{chatRoomId}/detail")
    public ResponseEntity<ChatRoomInfoResponseDto> getChatRoomDetail(@PathVariable Long chatRoomId) {


    }*/
}
