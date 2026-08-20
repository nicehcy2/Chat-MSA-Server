package com.nicehcy2.service;

import com.nicehcy2.common.error.exception.UserHandler;
import com.nicehcy2.common.util.JwtUtil;
import com.nicehcy2.dto.CustomUserInfoDto;
import com.nicehcy2.dto.LoginRequestDto;
import com.nicehcy2.dto.LoginResponseDto;
import com.nicehcy2.dto.RedisSessionDto;
import com.nicehcy2.dto.SignupRequestDto;
import com.nicehcy2.entity.User;
import com.nicehcy2.entity.enums.UserRole;
import com.nicehcy2.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    RedisTemplate<String, RedisSessionDto> redisTemplate;

    @Mock
    ValueOperations<String, RedisSessionDto> valueOperations;

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

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        // User 객체의 getter 호출 결과를 미리 정의
        when(user.getPassword()).thenReturn("1234");

        // 비밀번호 비교 결과를 true로 설정 (로그인 성공 상황)
        when(encoder.matches(password, user.getPassword()))
                .thenReturn(true);

        // Redis 저장 경로 stub
        when(jwtUtil.getREFRESH_TTL()).thenReturn(Duration.ofDays(1));
        when(jwtUtil.generateJTI()).thenReturn("JTI");
        when(jwtUtil.generateFamilyId()).thenReturn("FAMILY_ID");
        when(jwtUtil.generateRefreshToken()).thenReturn("REFRESH_TOKEN");
        when(jwtUtil.generateSHA256Token(anyString())).thenReturn("RT_HASH");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // JWT 생성 시 항상 "TOKEN" 문자열 반환하도록 설정
        when(jwtUtil.createAccessToken(any(CustomUserInfoDto.class), anyString()))
                .thenReturn("TOKEN");

        // === when ===
        LoginResponseDto jwtToken = authService.login(loginRequestDto);

        // === then ===
        assertEquals("TOKEN", jwtToken.accessToken());
        assertEquals("REFRESH_TOKEN", jwtToken.refreshToken());
        assertEquals("FAMILY_ID", jwtToken.sessionId());
    }

    @Test
    void login_실패_패스워드_불일치() {

        // given
        String email = loginRequestDto.email();
        String password = loginRequestDto.password();

        User user = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(user.getPassword()).thenReturn("encoded");

        // 비밀번호 비교 결과를 false로 설정 (로그인 실패 상황)
        when(encoder.matches(password, user.getPassword()))
                .thenReturn(false);

        // === when / then ===
        assertThrows(
                UserHandler.class,
                () -> authService.login(loginRequestDto)
        );
    }

    @Test
    void login_실패_이메일_없음() {

        // given
        when(userRepository.findByEmail(loginRequestDto.email()))
                .thenReturn(Optional.empty());

        // === when / then ===
        assertThrows(
                UserHandler.class,
                () -> authService.login(loginRequestDto)
        );
    }

    @Test
    void 회원가입_성공() {

        // === given ===
        SignupRequestDto signup = SignupRequestDto
                .builder()
                .email("nicehcy222@naver.com")
                .password("1234")
                .gender("M")
                .imageUrl("heo.jpg")
                .nickname("heo")
                .build();

        User savedUser = User.builder()
                .userId(1L)
                .email("nicehcy222@naver.com")
                .password("*1234")
                .gender("M")
                .imageUrl("heo.jpg")
                .nickname("heo")
                .userRole(UserRole.USER)
                .build();

        when(userRepository.existsByEmail(signup.email()))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);
        when(encoder.encode(signup.password()))
                .thenReturn("*1234");

        // === when ===
        Long userId = authService.signup(signup);

        // === then ===
        assertEquals(savedUser.getUserId(), userId);
    }

    @Test
    void 회원가입_실패_이메일_중복() {

        // === given ===
        SignupRequestDto signup = SignupRequestDto
                .builder()
                .email("nicehcy222@naver.com")
                .password("1234")
                .gender("M")
                .imageUrl("heo.jpg")
                .nickname("heo")
                .build();

        when(userRepository.existsByEmail(signup.email()))
                .thenReturn(true);

        // === when / then ===
        assertThrows(UserHandler.class,
                () -> authService.signup(signup));
    }
}
