package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.dto.ChatRoomDetailDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomInfoResponseDto;
import com.nicehcy2.chatapiservice.dto.ChatRoomParticipantDto;
import com.nicehcy2.chatapiservice.dto.CreateChatRoomRequestDto;
import com.nicehcy2.chatapiservice.dto.ExploreChatRoomRequestDto;
import com.nicehcy2.chatapiservice.dto.ExploreRoomResponseDto;
import com.nicehcy2.chatapiservice.dto.JoinChatRoomRequestDto;
import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.service.ChatApiService;
import com.nicehcy2.chatapiservice.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatApiController {

    private final ChatApiService chatApiService;
    private final ChatRoomService chatRoomService;

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

    @GetMapping
    public ResponseEntity<List<ChatRoomInfoResponseDto>> getChatRoomList(@RequestHeader("X-User-Id") Long userId) {

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

    @GetMapping("/explore")
    public ResponseEntity<List<ExploreRoomResponseDto>> exploreChatRooms(
            @Valid @ModelAttribute ExploreChatRoomRequestDto request,
            @RequestHeader("X-User-Id") Long requesterId) {

        return ResponseEntity.ok(chatApiService.exploreChatRooms(requesterId, request));
    }

    // 가입 전 상세. membershipStatus가 요청자 기준이라 X-User-Id가 필수다
    @GetMapping("/{chatRoomId}/detail")
    public ResponseEntity<ChatRoomDetailDto> getChatRoomDetail(
            @PathVariable Long chatRoomId,
            @RequestHeader("X-User-Id") Long requesterId) {

        return ResponseEntity.ok(chatApiService.getChatRoomDetail(chatRoomId, requesterId));
    }

    @PostMapping
    public ResponseEntity<Long> createChatRoom(
            @Valid @RequestBody CreateChatRoomRequestDto request,
            @RequestHeader("X-User-Id") Long requesterId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatRoomService.createChatRoom(requesterId, request));
    }

    // body는 비공개방일 때만 의미 있다. 공개방 참여는 body 없이 호출한다
    @PostMapping("/{chatRoomId}/join")
    public ResponseEntity<Long> joinChatRoom(
            @PathVariable Long chatRoomId,
            @RequestBody(required = false) JoinChatRoomRequestDto request,
            @RequestHeader("X-User-Id") Long requesterId) {

        String password = request == null ? null : request.password();
        return ResponseEntity.ok(chatRoomService.joinChatRoom(requesterId, chatRoomId, password));
    }

}
