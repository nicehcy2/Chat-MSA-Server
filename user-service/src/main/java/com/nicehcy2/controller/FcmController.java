package com.nicehcy2.controller;

import com.nicehcy2.dto.FcmTokenRequestDto;
import com.nicehcy2.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    /**
     * 프론트에서 발급받은 FCM을 User DB에 저장
     * @return
     */
    @PostMapping("/token")
    public ResponseEntity<Long> saveFcmToken(@RequestBody final FcmTokenRequestDto fcmTokenRequestDto) {

        log.info("FCM 저장 로직 시작");
        return ResponseEntity.ok(fcmService.saveFcmToken(fcmTokenRequestDto));
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> deleteFcmToken() {

        return ResponseEntity.ok().build();
    }
}
