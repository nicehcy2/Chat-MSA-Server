package com.nicehcy2.service;

import com.nicehcy2.common.util.CookieUtil;
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

import java.time.Instant;
import java.util.Optional;

import static com.nicehcy2.common.util.JwtUtil.REFRESH_TTL;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, RedisSessionDto> redisTemplate;

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

        // 로그인에 성공하면 AccessToken을 생성해서 클라이언트에 응답해줌.
        // RefreshToken을 생성해서 Redis에 저장하고, 클라이언트에는 쿠키로 보내준다.
        // 클라이언트는 RefreshToken을 이용해서 짧은 만료기간을 가진 AccessToken을 재발급 받을 수 있다.

        // 1. SessionId, RefreshToken 생성, RefreshToken을 해시로 변환
        String familyId = jwtUtil.generateFamilyId(); // SessionID 생성
        String refreshToken = jwtUtil.generateRefreshToken(); // Refresh Token 생성
        String rtHash = jwtUtil.generateSHA256Token(refreshToken); // refresh Token을 해시로 변환
        long rtExp = Instant.now().plus(REFRESH_TTL).getEpochSecond(); // refresh Token 만료 기간

        // 2. Redis에 저장할 User 정보 객체 생성
        CustomUserInfoDto info = CustomUserInfoDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getUserRole())
                .sessionId(familyId)
                .build();

        // 3. Redis에 저장할 User 정보, RefreshToken 해시값, 만료기간 객체 생성.
        RedisSessionDto redisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(info)
                .rtHash(rtHash)
                .prevRtHash(null)       // 최초 로그인 시 이전 토큰 없음
                .rotatedAtEpoch(null)
                .expiresAtEpoch(rtExp)
                .build();

        // 4. Redis에 저장.
        // Redis에 User 정보와 sessionId, refreshToken, 만료기간을 기록한다.
        redisTemplate.opsForValue().set("rt:session:" + familyId, redisSessionDto, REFRESH_TTL);

        // RefreshToken과 SessionID, 유저 정보를 기반으로 AccessToken을 만들고 반환.
        return LoginResponseDto.builder()
                .accessToken(jwtUtil.createAccessToken(info))
                .userId(user.getUserId())
                .refreshToken(refreshToken)
                .sessionId(familyId)
                .build();
    }

    @Transactional
    public LoginResponseDto refresh(String refreshToken, String sessionId) {

        // 클라이언트가 쿠키로 보낸 refreshToken을 hash로 변환.
        String incomingRtHash = jwtUtil.generateSHA256Token(refreshToken);

        // 1. 세션 조회 (Redis에 동일한 키가 있는지 확인)
        RedisSessionDto sessionDto = Optional.ofNullable(
                redisTemplate.opsForValue().get("rt:session:" + sessionId)
        ).orElseThrow(() -> new RuntimeException("세션이 존재하지 않습니다."));

        // 2. 현재 RT 검증 (현재 유효한 해시인지 비교)
        // Refresh 되면 rtHash 값이 바뀐다. 즉, 이전 RefreshToken을 사용할 경우, 인증에 실패한다.
        boolean isCurrentRt = sessionDto.rtHash().equals(incomingRtHash);

        // 3. 이전 RT 검증 (overlap 허용)
        // 재사용 탐지: 새로운 토큰이 생성 되었는데, 이전 토큰이 사용됨
        // 네트워크 문제나 동시 refresh를 할 경우, 정상적인 접근에도 인증에 실패할 수 있다.
        // 조금의 오차를 허용해줘서 사용자 경험을 개선한다.
        boolean isPrevRt = false;
        if (!isCurrentRt && sessionDto.prevRtHash() != null && sessionDto.rotatedAtEpoch() != null) {

            long now = Instant.now().getEpochSecond();
            long secondsSinceRotation = now - sessionDto.rotatedAtEpoch();

            isPrevRt = sessionDto.prevRtHash().equals(incomingRtHash)
                    && secondsSinceRotation <= JwtUtil.OVERLAP_WINDOW.toSeconds();
        }

        // 4. 둘 다 아니면 -> 재사용 공격 또는 만료
        if (!isCurrentRt && !isPrevRt) {

            // 재사용 탐지 -> 세션 전체 삭제
            redisTemplate.delete("rt:session:" + sessionId);
            throw new RuntimeException("Invalid or reused refresh token. Session revoked.");
        }

        // 5. 회전(새로운 RT 발급)
        String newRefreshToken = jwtUtil.generateRefreshToken(); // 새로운 refreshToken 발급
        String newRtHash = jwtUtil.generateSHA256Token(newRefreshToken); // 새로운 rtHash 발급
        long newRtExp = Instant.now().plus(REFRESH_TTL).getEpochSecond();
        long now = Instant.now().getEpochSecond();

        // AccessToken 생성용 User 정보 초기화
        CustomUserInfoDto customUserInfoDto = CustomUserInfoDto.builder()
                .userId(sessionDto.customUserInfoDto().userId())
                .email(sessionDto.customUserInfoDto().email())
                .role(sessionDto.customUserInfoDto().role())
                .sessionId(sessionId)
                .build();

        // Redis에 저장할 세션, RefreshToken 정보
        RedisSessionDto newRedisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(customUserInfoDto)
                .rtHash(newRtHash)
                // 이전 RT 기록: overlap 요청이었으면 prevRtHash 유지, 정상 rotate면 현재 걸 prev로
                .prevRtHash(isCurrentRt ? sessionDto.rtHash() : sessionDto.prevRtHash())
                .rotatedAtEpoch(isCurrentRt ? now : sessionDto.rotatedAtEpoch()) // prevRt면 갱신 안 함
                .expiresAtEpoch(newRtExp)
                .build();

        // Redis에 새로운 세션 + refresh Token 저장
        redisTemplate.opsForValue().set("rt:session:" + sessionId, newRedisSessionDto, REFRESH_TTL);

        String newAccessToken = jwtUtil.createAccessToken(customUserInfoDto); // 새로운 AccessToken 발급
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
                .ageGroup(signupRequestDto.ageGroup())
                .reward(signupRequestDto.reward())
                .birthDay(signupRequestDto.birthDay())
                .userRole(signupRequestDto.userRole())
                .jobGroup(signupRequestDto.jobGroup())
                .gender(signupRequestDto.gender())
                .userRole(signupRequestDto.userRole())
                .email(signupRequestDto.email())
                .imageUrl(signupRequestDto.imageUrl())
                .build();

        User saved = userRepository.save(user);
        return saved.getUserId();
    }

    public boolean checkEmailDuplicate(String email) {

        return userRepository.existsByEmail(email);
    }
}
