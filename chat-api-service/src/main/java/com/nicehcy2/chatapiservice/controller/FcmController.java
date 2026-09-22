package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.dto.FcmTokenRequestDto;
import com.nicehcy2.chatapiservice.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats/fcm")
public class FcmController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    public ResponseEntity<Long> register(
            @Valid @RequestBody FcmTokenRequestDto request,
            @RequestHeader("X-User-Id") Long requesterId) {

        return ResponseEntity.ok(fcmTokenService.register(requesterId, request));
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> delete(
            @RequestParam String token,
            @RequestHeader("X-User-Id") Long requesterId) {

        fcmTokenService.delete(requesterId, token);
        return ResponseEntity.noContent().build();
    }
}
