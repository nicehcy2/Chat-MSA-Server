package com.nicehcy2.service;

import com.nicehcy2.common.util.JwtUtil;
import com.nicehcy2.dto.*;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    // TODO: 추후에 설정 파일에서 세팅하도록 변경
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        String email = requestDto.email();
        String password = requestDto.password();
        User user = userRepository.findUserByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("이메일이 존재하지 않습니다.");
        }

        if (!encoder.matches(password, user.getPassword())) {

            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        // Refresh Token 생성
        String familyId = jwtUtil.generateFamilyId();
        String refreshToken = jwtUtil.generateRefreshToken();
        String rtHash = jwtUtil.generateSHA256Token(refreshToken); // refresh Token을 해시로 변환

        long rtExp = Instant.now().plus(REFRESH_TTL).getEpochSecond(); // refresh Token 만료 기간

        CustomUserInfoDto info = CustomUserInfoDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getUserRole())
                .build();

        // TODO: Redis 리팩토링 반드시 필요
        RedisSessionDto redisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(info)
                .rtHash(rtHash)
                .expiresAtEpoch(rtExp)
                .build();

        redisTemplate.opsForValue().set("rt:session:" + familyId, redisSessionDto, REFRESH_TTL);

        // TODO: access Token에 family Id 추가
        // TODO: 왜 디바이스가 다르면 sesionId(=family Id)도 달라질까?
        return LoginResponseDto.builder()
                .accessToken(jwtUtil.createAccessToken(info))
                .userId(user.getUserId())
                .refreshToken(refreshToken)
                .sessionId(familyId)
                .build();
    }

    @Transactional
    public LoginResponseDto refresh(String refreshToken, String sessionId) {

        String incomingRtHash = jwtUtil.generateSHA256Token(refreshToken);

        // 1. 재사용 탐지: 새로운 토큰이 생성 되었는데, 이전 토큰이 사용됨
        // TODO: overlap을 사용하면 이 부분 수정이 필요함.

        // 2. 세션 조회
        // TODO: null일 경우 오류 발생. null이면 타입 캐스팅 자체가 불가능.
        RedisSessionDto sessionDto = (RedisSessionDto) redisTemplate.opsForValue().get("rt:session:" + sessionId);
        if (sessionDto == null) throw new RuntimeException("sessionDto가 없습니다.");

        // 3. 해시 비교(현재 유요한 RT인지 확인)
        if (!sessionDto.rtHash().equals(incomingRtHash)) {
            throw new RuntimeException("invalid refresh token");
        }

        // 4. 회전(새로운 RT 발급)
        // 기존 RT를 used로 마킹(재사용 탐지용)
        // 새 RT 발급 후 session의 rtHash 교체
        // TODO: used로 마킹하는 로직 추가

        String newRefreshToken = jwtUtil.generateRefreshToken();
        String newRtHash = jwtUtil.generateSHA256Token(newRefreshToken);
        long newRtExp = Instant.now().plus(REFRESH_TTL).getEpochSecond();

        // AccessToken 생성용 User 정보 초기화
        CustomUserInfoDto customUserInfoDto = CustomUserInfoDto.builder()
                .userId(sessionDto.customUserInfoDto().userId())
                .email(sessionDto.customUserInfoDto().email())
                .role(sessionDto.customUserInfoDto().role())
                .build();

        // Redis에 저장할 세션 정보
        RedisSessionDto newRedisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(customUserInfoDto)
                .rtHash(newRtHash)
                .expiresAtEpoch(newRtExp)
                .build();
        // TODO: 어떻게 sessionID는 refresh 전과 동일해도 될까?
        redisTemplate.opsForValue().set("rt:session:" + sessionId, newRedisSessionDto, REFRESH_TTL);

        String newAccessToken = jwtUtil.createAccessToken(customUserInfoDto);
        return LoginResponseDto.builder()
                .refreshToken(newRefreshToken)
                .sessionId(sessionId)
                .accessToken(newAccessToken)
                .userId(sessionDto.customUserInfoDto().userId())
                .build();
    }

    public Long signup(SignupRequestDto signupRequestDto) {

        User exist = userRepository.findUserByEmail(signupRequestDto.email());
        if (exist != null) {
            throw new RuntimeException("이메일이 이미 존재합니다");
        }

        String encodePassword = encoder.encode(signupRequestDto.password());

        User user = User.builder()
                .nickname(signupRequestDto.nickname())
                .password(encodePassword)
                .gender(signupRequestDto.gender())
                .userRole(signupRequestDto.userRole())
                .email(signupRequestDto.email())
                .imageUrl(signupRequestDto.imageUrl())
                .build();

        User saved = userRepository.save(user);
        return saved.getUserId();
    }
}
