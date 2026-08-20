package com.nicehcy2.service;

import com.nicehcy2.common.error.ResponseCode;
import com.nicehcy2.common.error.exception.RedisHandler;
import com.nicehcy2.common.error.exception.UserHandler;
import com.nicehcy2.common.util.JwtUtil;
import com.nicehcy2.common.util.RedisKeyNamingUtil;
import com.nicehcy2.dto.*;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, RedisSessionDto> redisTemplate;

    public LoginResponseDto login(LoginRequestDto requestDto) {

        final String email = requestDto.email();
        final String password = requestDto.password();

        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ResponseCode.USER_NOT_FOUND));

        // 비밀번호가 일치하지 않으면 예외 던짐
        if (!encoder.matches(password, user.getPassword())) {
            throw new UserHandler(ResponseCode.USER_PASSWORD_MISMATCH);
        }

        // 로그인에 성공하면 AccessToken을 생성해서 클라이언트에 응답해줌.
        // RefreshToken을 생성해서 Redis에 저장하고, 클라이언트에는 쿠키로 보내준다.
        // 클라이언트는 RefreshToken을 이용해서 짧은 만료기간을 가진 AccessToken을 재발급 받을 수 있다.

        // 1. jti, SessionId(FamilyId), RefreshToken 생성, RefreshToken을 해시로 변환
        final String jti = jwtUtil.generateJTI();
        final String familyId = jwtUtil.generateFamilyId(); // SessionID 생성
        final String refreshToken = jwtUtil.generateRefreshToken(); // Refresh Token 생성
        final String rtHash = jwtUtil.generateSHA256Token(refreshToken); // refresh Token을 해시로 변환
        final long rtExp = Instant.now().plus(jwtUtil.getREFRESH_TTL()).getEpochSecond(); // refresh Token 만료 기간

        // 2. Redis에 저장할 User 정보 객체 생성
        final CustomUserInfoDto info = CustomUserInfoDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getUserRole())
                .sessionId(familyId)
                .build();

        // 3. Redis에 저장할 User 정보, RefreshToken 해시값, 만료기간 객체 생성.
        final RedisSessionDto redisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(info)
                .rtHash(rtHash)
                .currentAccessJti(jti)
                .prevRtHash(null)       // 최초 로그인 시 이전 토큰 없음
                .rotatedAtEpoch(null)
                .expiresAtEpoch(rtExp)
                .build();

        // 4. Redis에 저장.
        // Redis에 User 정보와 sessionId, refreshToken, 만료기간을 기록한다.
        redisTemplate.opsForValue()
                .set(RedisKeyNamingUtil.refreshTokenKey(familyId), redisSessionDto, jwtUtil.getREFRESH_TTL());

        // SessionID, 유저 정보를 기반으로 AccessToken을 만들고 반환.
        return LoginResponseDto.builder()
                .accessToken(jwtUtil.createAccessToken(info, jti))
                .userId(user.getUserId())
                .refreshToken(refreshToken)
                .sessionId(familyId)
                .build();
    }

    // TODO: 세션 단위 Redis 분산 잠금 추가 (SET NX PX + 토큰 비교 해제)
    //  아래 조회→검증→저장이 비원자적이라, ms 단위로 동시에 도착한 current-RT refresh 2건이
    //  둘 다 회전 경로로 들어가 서로를 고아로 만들 수 있다. (최악: 재사용 오판 → 재로그인 1회)
    //  prev-RT 무회전 처리로 30초 overlap 구간은 이미 방어됨 → 저확률 케이스라 후순위.
    public LoginResponseDto refresh(String refreshToken, String sessionId) {

        // 클라이언트가 쿠키로 보낸 refreshToken을 hash로 변환.
        final String incomingRtHash = jwtUtil.generateSHA256Token(refreshToken);

        // 1. 세션 조회 (Redis에 동일한 키가 있는지 확인)
        final RedisSessionDto sessionDto = Optional.ofNullable(
                redisTemplate.opsForValue().get(RedisKeyNamingUtil.refreshTokenKey(sessionId))
        ).orElseThrow(() -> new RedisHandler(ResponseCode.SESSION_NOT_FOUND));

        final long now = Instant.now().getEpochSecond();

        // 1-1. 절대 만료 검증
        // 로그인 시점에 정한 만료 시각은 refresh로 연장되지 않는다.
        // (이 검증이 없으면 주기적으로 refresh만 해도 세션이 영구히 유지된다)
        if (now >= sessionDto.expiresAtEpoch()) {
            redisTemplate.delete(RedisKeyNamingUtil.refreshTokenKey(sessionId));
            throw new RedisHandler(ResponseCode.SESSION_EXPIRED);
        }

        // 2. 현재 RT 검증 (현재 유효한 해시인지 비교)
        // Refresh 되면 rtHash 값이 바뀐다. 즉, 이전 RefreshToken을 사용할 경우, 인증에 실패한다.
        final boolean isCurrentRt = sessionDto.rtHash().equals(incomingRtHash);

        // 3. 이전 RT 검증 (overlap 허용)
        // 재사용 탐지: 새로운 토큰이 생성 되었는데, 이전 토큰이 사용됨
        // 네트워크 문제나 동시 refresh를 할 경우, 정상적인 접근에도 인증에 실패할 수 있다.
        // 조금의 오차를 허용해줘서 사용자 경험을 개선한다.
        boolean isPrevRt = false;
        if (!isCurrentRt && sessionDto.prevRtHash() != null && sessionDto.rotatedAtEpoch() != null) {

            long secondsSinceRotation = now - sessionDto.rotatedAtEpoch();

            isPrevRt = sessionDto.prevRtHash().equals(incomingRtHash)
                    && secondsSinceRotation <= jwtUtil.getOVERLAP_WINDOW().toSeconds();
        }

        // 4. 둘 다 아니면 -> 재사용 공격 또는 만료
        if (!isCurrentRt && !isPrevRt) {

            // 재사용 탐지 -> 세션 전체 삭제
            redisTemplate.delete(RedisKeyNamingUtil.refreshTokenKey(sessionId));
            throw new RedisHandler(ResponseCode.SESSION_REUSE_DETECTED);
        }

        // AccessToken 생성용 User 정보 초기화
        final CustomUserInfoDto customUserInfoDto = CustomUserInfoDto.builder()
                .userId(sessionDto.customUserInfoDto().userId())
                .email(sessionDto.customUserInfoDto().email())
                .role(sessionDto.customUserInfoDto().role())
                .sessionId(sessionId)
                .build();

        // 5. prev-RT(overlap) 요청: 회전하지 않는다.
        // 이 요청은 동시 refresh에서 늦게 도착한 쪽이다. 여기서 회전해버리면
        // 먼저 도착한 요청이 방금 발급받은 current RT가 무효화되고,
        // 그 RT로 다음 refresh를 하는 순간 재사용 공격으로 오판되어 강제 로그아웃된다.
        // 그래서 새 AccessToken만 발급하고, 세션과 쿠키는 건드리지 않는다.
        if (isPrevRt) {

            final String jtiForPrev = jwtUtil.generateJTI();
            return LoginResponseDto.builder()
                    .refreshToken(null) // 회전 없음 → 컨트롤러가 쿠키를 재설정하지 않는다
                    .sessionId(sessionId)
                    .accessToken(jwtUtil.createAccessToken(customUserInfoDto, jtiForPrev))
                    .userId(sessionDto.customUserInfoDto().userId())
                    .build();
        }

        // 6. 정상 회전(새로운 RT 발급) — 여기 도달하면 isCurrentRt == true
        final String jti = jwtUtil.generateJTI();
        final String newRefreshToken = jwtUtil.generateRefreshToken(); // 새로운 refreshToken 발급
        final String newRtHash = jwtUtil.generateSHA256Token(newRefreshToken); // 새로운 rtHash 발급

        // Redis에 저장할 세션, RefreshToken 정보
        final RedisSessionDto newRedisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(customUserInfoDto)
                .rtHash(newRtHash)
                .currentAccessJti(jti)
                .prevRtHash(sessionDto.rtHash()) // 방금까지의 current를 prev로 (overlap 허용 대상)
                .rotatedAtEpoch(now)
                .expiresAtEpoch(sessionDto.expiresAtEpoch()) // 절대 만료는 회전해도 그대로 유지
                .build();

        // Redis에 새로운 세션 + refresh Token 저장
        // TTL은 절대 만료까지 남은 시간으로 설정한다. (REFRESH_TTL로 다시 설정하면 만료가 계속 밀린다)
        redisTemplate.opsForValue()
                .set(RedisKeyNamingUtil.refreshTokenKey(sessionId), newRedisSessionDto,
                        Duration.ofSeconds(sessionDto.expiresAtEpoch() - now));

        final String newAccessToken = jwtUtil.createAccessToken(customUserInfoDto, jti); // 새로운 AccessToken 발급
        return LoginResponseDto.builder()
                .refreshToken(newRefreshToken)
                .sessionId(sessionId)
                .accessToken(newAccessToken)
                .userId(sessionDto.customUserInfoDto().userId())
                .build();
    }

    @Transactional
    public Long signup(SignupRequestDto signupRequestDto) {

        // 동일한 이메일을 가진 유저가 존재하면 에러 처리
        // (동시 요청 대비 최종 방어선은 users.email의 unique 제약)
        if (userRepository.existsByEmail(signupRequestDto.email())) {
            throw new UserHandler(ResponseCode.USER_ALREADY_EXISTS);
        }

        final String encodedPassword = encoder.encode(signupRequestDto.password());

        final User user = User.of(signupRequestDto, encodedPassword);
        final User saved = userRepository.save(user);

        return saved.getUserId();
    }

    public boolean checkEmailDuplicate(String email) {

        return userRepository.existsByEmail(email);
    }

    /**
     * 로그아웃: 세션(토큰 family) 전체를 폐기한다.
     * 이미 세션이 없어도 성공으로 처리한다. (멱등)
     */
    public void logout(String sessionId) {

        redisTemplate.delete(RedisKeyNamingUtil.refreshTokenKey(sessionId));
    }
}
