package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.dto.MessageDto;
import com.nicehcy2.chatapiservice.dto.ChatServerInfoResponse;
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
     *
     * @return 클라이언트에게 적절한 채팅 서버 정보(주소)를 반환
     */
    @PostMapping("/assign")
    public ResponseEntity<ChatServerInfoResponse> assignChatServer() {

        return ResponseEntity.ok(chatApiService.assignChatServer());
    }

    // 동기화 로직 적용 전 임시 메시지 조회 코드
    @GetMapping("/{chatRoomId}/messages/test")
    public ResponseEntity<List<MessageDto>> getChatMessages(@PathVariable Long chatRoomId) {

        return ResponseEntity.ok(chatApiService.getChatMessages(chatRoomId));
    }
}
