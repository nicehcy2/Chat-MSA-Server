package com.nicehcy2.controller;

import com.nicehcy2.dto.MyPageUserInfoResponseDto;
import com.nicehcy2.dto.UserInfoRequestDto;
import com.nicehcy2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    // TODO(배포 전 필수): userId를 클라이언트에게 받지 말고 게이트웨이가 심어주는
    //  X-User-Id 헤더(@RequestHeader)로 전환할 것. 지금은 아무나 남의 userId로
    //  조회/수정이 가능하다. 전환 시 게이트웨이 JwtAuthorizationFilter의
    //  h.add() → h.set() 교체(클라이언트가 보낸 X-User-Id 위조 방지)도 함께 필요.

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<MyPageUserInfoResponseDto> getUserInfo(@PathVariable Long userId) {

        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @PatchMapping("/profile/edit")
    public ResponseEntity<Void> updateUserProfile(@RequestParam Long userId,
                                                  @RequestBody UserInfoRequestDto userInfoRequestDto) {

        userService.modifyUserProfile(userId, userInfoRequestDto);
        return ResponseEntity.ok().build();
    }
}
