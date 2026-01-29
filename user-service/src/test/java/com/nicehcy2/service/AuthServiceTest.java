package com.nicehcy2.service;

import com.nicehcy2.common.util.JwtUtil;
import com.nicehcy2.dto.CustomUserInfoDto;
import com.nicehcy2.dto.LoginRequestDto;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    // 가짜 UserRepository 객체를 만듦.
    UserRepository userRepository;

    @Mock
    PasswordEncoder encoder;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    // 테스트 대상 "실제 객체"
    // 위의 @Mock 들이 생성자 기준으로 자동 주입됨
    AuthService authService;

    LoginRequestDto loginRequestDto;

    @BeforeEach
    void setUpLoginRequestDto() {
        loginRequestDto = LoginRequestDto.builder()
                .email("nicehcy2@naver.com")
                .password("1234")
                .build();
    }

    @Test
    void login_성공() {

        // given
        String email = loginRequestDto.email();
        String password = loginRequestDto.password();

        // User 엔티티도 실제 DB 대신 mock 사용
        User user = mock(User.class);

        when(userRepository.findUserByEmail(email))
                .thenReturn(user);

        // User 객체의 getter 호출 결과를 미리 정의
        when(user.getPassword()).thenReturn("1234");

        // 비밀번호 비교 결과를 true로 설정 (로그인 성공 상황)
        // 어차피 encode는 실제 코드에서 알아서 해준다. 디코드 했을 때 같은 값이면 true가 나와야 됨.
        when(encoder.matches(user.getPassword(), password))
                .thenReturn(true);
        // JWT 생성 시 항상 "TOKEN" 문자열 반환하도록 설정
        when(jwtUtil.createAccessToken(any(CustomUserInfoDto.class)))
                .thenReturn("TOKEN");

        // === when ===
        String jwtToken = authService.login(loginRequestDto);

        // === then ===
        assertEquals("TOKEN", jwtToken);
    }

    @Test
    void login_실패_패스워드_불일치() {

        // given
        String email = loginRequestDto.email();
        String password = loginRequestDto.password();

        User user = mock(User.class);

        when(userRepository.findUserByEmail(email))
                .thenReturn(user);
        when(user.getPassword()).thenReturn(password);

        // 비밀번호 비교 결과를 true로 설정 (로그인 성공 상황)
        // 어차피 encode는 실제 코드에서 알아서 해준다. 디코드 했을 때 같은 값이면 true가 나와야 됨.
        when(encoder.matches(user.getPassword(), password))
                .thenReturn(false);

        // === when ===
        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequestDto)
        );
    }
}