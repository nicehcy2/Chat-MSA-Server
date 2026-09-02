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

    // 동기화 로직 적용 전 임시 메시지 조회 코드
    // TODO(보안): 멤버십 검증이 없어 임의 방의 메시지를 조회할 수 있다. 정식 동기화 API로 교체 시 함께 해결할 것.
    @GetMapping("/{chatRoomId}/messages/test")
    public ResponseEntity<List<MessageDto>> getChatMessages(@PathVariable Long chatRoomId) {

        return ResponseEntity.ok(chatApiService.getChatMessages(chatRoomId));
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
