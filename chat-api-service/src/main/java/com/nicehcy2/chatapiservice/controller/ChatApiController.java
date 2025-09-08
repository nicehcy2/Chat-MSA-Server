package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.dto.ChatServerInfoResponse;
import com.nicehcy2.chatapiservice.service.ChatApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
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
}
