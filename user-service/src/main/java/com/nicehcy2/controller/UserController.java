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
