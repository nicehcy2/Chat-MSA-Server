package com.nicehcy2.controller;

import com.nicehcy2.common.util.CookieUtil;
import com.nicehcy2.dto.*;
import com.nicehcy2.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("login")
    public ResponseEntity<AccessTokenResponseDto> createAuthToken(
            @Valid @RequestBody LoginRequestDto requestDto,
            HttpServletResponse response) {

        LoginResponseDto loginResponse = authService.login(requestDto);

        // Cookie 추가
        CookieUtil.addAuthCookies(
                response,
                loginResponse.refreshToken(),
                loginResponse.sessionId());

        AccessTokenResponseDto accessTokenResponseDto = AccessTokenResponseDto.builder()
                .accessToken(loginResponse.accessToken())
                .userId(loginResponse.userId())
                .build();

        return ResponseEntity.ok(accessTokenResponseDto);
    }

    @PostMapping("refresh")
    public ResponseEntity<RefreshResponseDto> createRefreshToken(
            @CookieValue("refreshToken") String refreshToken,
            @CookieValue("sessionId") String sessionId,
            HttpServletResponse response
    ) {

        LoginResponseDto responseDto = authService.refresh(refreshToken, sessionId);
        CookieUtil.addAuthCookies(response,
                responseDto.refreshToken(),
                responseDto.sessionId());

        return ResponseEntity.ok(new RefreshResponseDto(responseDto.accessToken()));
    }

    @PostMapping("signup")
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {

        return ResponseEntity.ok(authService.signup(signupRequestDto));
    }
}
