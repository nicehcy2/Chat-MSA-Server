package com.nicehcy2.controller;

import com.nicehcy2.dto.LoginRequestDto;
import com.nicehcy2.dto.SignupRequestDto;
import com.nicehcy2.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("login")
    public ResponseEntity<String> createAuthToken(@Valid @RequestBody LoginRequestDto requestDto) {

        return ResponseEntity.ok(authService.login(requestDto));
    }

    @PostMapping("signup")
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {

        return ResponseEntity.ok(authService.signup(signupRequestDto));
    }
}
