package com.nicehcy2.service;

import com.nicehcy2.common.error.ResponseCode;
import com.nicehcy2.common.error.exception.RedisHandler;
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
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        UserHandler e = assertThrows(
                UserHandler.class,
                () -> authService.login(loginRequestDto)
        );
        assertEquals(ResponseCode.USER_PASSWORD_MISMATCH, e.getErrorCode());
    }

    @Test
    void login_실패_이메일_없음() {

        // given
        when(userRepository.findByEmail(loginRequestDto.email()))
                .thenReturn(Optional.empty());

        // === when / then ===
        UserHandler e = assertThrows(
                UserHandler.class,
                () -> authService.login(loginRequestDto)
        );
        assertEquals(ResponseCode.USER_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void refresh_실패_절대만료() {

        // given: 절대 만료 시각이 이미 지난 세션
        RedisSessionDto expiredSession = RedisSessionDto.builder()
                .rtHash("RT_HASH")
                .expiresAtEpoch(Instant.now().getEpochSecond() - 10)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(expiredSession);

        // === when / then ===
        RedisHandler e = assertThrows(
                RedisHandler.class,
                () -> authService.refresh("REFRESH_TOKEN", "SESSION_ID")
        );
        assertEquals(ResponseCode.SESSION_EXPIRED, e.getErrorCode());

        // 만료된 세션은 Redis에서 삭제되어야 한다
        verify(redisTemplate).delete("rt:session:SESSION_ID");
    }

    @Test
    void refresh_성공_현재RT는_회전() {

        // given: current RT로 정상 refresh
        RedisSessionDto session = RedisSessionDto.builder()
                .customUserInfoDto(CustomUserInfoDto.builder().userId(1L).build())
                .rtHash("CURRENT_HASH")
                .expiresAtEpoch(Instant.now().getEpochSecond() + 3600)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(session);
        when(jwtUtil.generateSHA256Token("CURRENT_RT")).thenReturn("CURRENT_HASH");
        when(jwtUtil.generateJTI()).thenReturn("JTI");
        when(jwtUtil.generateRefreshToken()).thenReturn("NEW_RT");
        when(jwtUtil.generateSHA256Token("NEW_RT")).thenReturn("NEW_HASH");
        when(jwtUtil.createAccessToken(any(CustomUserInfoDto.class), anyString()))
                .thenReturn("NEW_AT");

        // === when ===
        LoginResponseDto result = authService.refresh("CURRENT_RT", "SESSION_ID");

        // === then ===
        assertEquals("NEW_RT", result.refreshToken()); // 회전됨 → 새 RT 반환 (쿠키 재설정 대상)
        assertEquals("NEW_AT", result.accessToken());

        // 회전된 세션이 Redis에 저장되어야 한다
        verify(valueOperations).set(eq("rt:session:SESSION_ID"), any(RedisSessionDto.class), any(Duration.class));
    }

    @Test
    void refresh_overlap_이전RT는_회전하지_않음() {

        // given: 방금 회전된 세션에 이전 RT(prev)로 refresh가 도착 (동시 refresh 시나리오)
        long now = Instant.now().getEpochSecond();
        RedisSessionDto session = RedisSessionDto.builder()
                .customUserInfoDto(CustomUserInfoDto.builder().userId(1L).build())
                .rtHash("CURRENT_HASH")
                .prevRtHash("PREV_HASH")
                .rotatedAtEpoch(now - 5) // 5초 전 회전 → overlap window(30초) 안
                .expiresAtEpoch(now + 3600)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(session);
        when(jwtUtil.generateSHA256Token("PREV_RT")).thenReturn("PREV_HASH");
        when(jwtUtil.getOVERLAP_WINDOW()).thenReturn(Duration.ofSeconds(30));
        when(jwtUtil.generateJTI()).thenReturn("JTI");
        when(jwtUtil.createAccessToken(any(CustomUserInfoDto.class), anyString()))
                .thenReturn("NEW_AT");

        // === when ===
        LoginResponseDto result = authService.refresh("PREV_RT", "SESSION_ID");

        // === then ===
        assertNull(result.refreshToken()); // 회전 없음 → 컨트롤러가 쿠키를 건드리지 않는다
        assertEquals("NEW_AT", result.accessToken()); // AT만 새로 발급

        // 세션은 변경되지 않아야 한다 (current RT 고아화 방지)
        verify(valueOperations, never()).set(anyString(), any(RedisSessionDto.class), any(Duration.class));
    }

    @Test
    void logout_세션_삭제() {

        // === when ===
        authService.logout("SESSION_ID");

        // === then ===
        verify(redisTemplate).delete("rt:session:SESSION_ID");
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
