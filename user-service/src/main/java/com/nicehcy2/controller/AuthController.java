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

        // RefreshToken과 SessionId는 쿠키를 사용하서 클라이언트로 보내준다.
        // HttpOnly를 사용해서 JS로 세션 정보를 탈취하지 못하도록 한다. XSS 공격 방지
        CookieUtil.addAuthCookies(
                response,
                loginResponse.refreshToken(),
                loginResponse.sessionId());

        // AccessToken은 응답으로 보내준다.
        // 클라이언트는 클라이언트의 메모리에 AccessToken을 보내준다.
        // AccessToken은 암호화가 되어 있는 토큰이 아니므로 중요한 개인정보를 저장하며 안된다.
        // 공격을 당할 가능성이 있기에 AccessToken의 만료 시간은 짧게 설정한다.
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

        return ResponseEntity.ok(RefreshResponseDto.builder()
                        .accessToken(responseDto.accessToken())
                        .sessionId(responseDto.sessionId())
                        .userId(responseDto.userId())
                .build());
    }

    @PostMapping("signup")
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {

        return ResponseEntity.ok(authService.signup(signupRequestDto));
    }

    @GetMapping("signup/email/check")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {

        return ResponseEntity.ok(authService.checkEmailDuplicate(email));
    }
}
