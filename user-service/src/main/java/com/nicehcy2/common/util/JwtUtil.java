package com.nicehcy2.common.util;

import com.nicehcy2.common.error.ResponseCode;
import com.nicehcy2.common.error.exception.JwtHandler;
import com.nicehcy2.dto.CustomUserInfoDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Getter
@Component
public class JwtUtil {

    private final Key KEY; // secretKey를 HMAC 알고리즘에 맞게 저장. 해시 서명용 비밀키
    private final long ACCESS_TOKEN_EXP_TIME; // accessToken 만료 시간
    private final Duration OVERLAP_WINDOW; // 오버랩 허용 시간
    private final Duration REFRESH_TTL; // Refresh Token 만료 시간

    // 무작위 토큰 생성을 위한 상수
    // Random은 예측이 가능하고 시드가 유추되면 다음 값도 예측 가능하기에 암호학적으로 안전한 SecureRandom 사용
    private static final SecureRandom RANDOM = new SecureRandom();

    public JwtUtil(
            @Value("${jwt.secret}") final String SECRET_KEY,
            @Value("${jwt.accesstoken-expiration-time}") final long ACCESS_TOKEN_EXP_TIME,
            @Value("${jwt.overlap-window}") final Duration OVERLAP_WINDOW,
            @Value("${jwt.refresh-ttl}") final Duration REFRESH_TTL
    ) {

        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY); // BASE64로 인코딩된 문자열을 바이트로 되돌림

        // JJWT에서 쓰는 코드인데, byte를 기반으로 HMAC 알고리즘에 맞는 Key 객체 생성
        // HMAC은 비밀키를 이용해서 데이터가 위조되지 않았음을 증명하는 위조 방지 서명이다. (해시 방식 암호화가 아니다)
        // 비밀키 + 메시지를 섞고 SHA-256 해시 계산을 하고 또 한 번 감싸서 해시 계산 -> 무결성 + 인증 보장
        this.KEY = Keys.hmacShaKeyFor(keyBytes);
        this.ACCESS_TOKEN_EXP_TIME = ACCESS_TOKEN_EXP_TIME;
        this.OVERLAP_WINDOW = OVERLAP_WINDOW;
        this.REFRESH_TTL = REFRESH_TTL;
    }

    /**
     * AccessToken 생성
     * @param user JWT 페이로드에 추가할 사용자 기본 정보
     * @param jti AccessToken 고유 식별자
     * @return Access Token String
     */
    public String createAccessToken(CustomUserInfoDto user, String jti) {
        return createToken(user, jti, ACCESS_TOKEN_EXP_TIME);
    }

    private String createToken(CustomUserInfoDto user, String jti, long expireTime) {

        // 1. JWT Payload를 만드는 코드
        // Claims는 JWT의 payload 영역을 의미. JWT 안에 사용자 정보를 넣는다.
        // 서버가 매번 DB를 조회하지 않고 사용자 정보 및 필요한 정보를 조회 가능
        Claims claims = Jwts.claims();
        claims.put("userId", user.userId());
        claims.put("email", user.email());
        claims.put("role", user.role());
        claims.put("sessionId", user.sessionId());

        Instant now = Instant.now();
        final Instant tokenValidity = now.plusSeconds(expireTime);

        return Jwts.builder()
                .setClaims(claims) // 사용자 정보를 JWT에 넣음
                .setId(jti) // jti 추가 (AccessToken 단위 추적용)
                .setIssuedAt(Date.from(now)) // JWT 발급 시간 (토큰 재사용 방지)
                .setExpiration(Date.from(tokenValidity)) // JWT 만료 시간
                .signWith(KEY, SignatureAlgorithm.HS256) // SHA-256 알고리즘으로 서명, KEY = 서버만 알고 있는 비밀 키
                .compact(); // JWT를 최종 문자열 형태로 생성
    }

    /**
     * Token에서 UserId 추출
     * @param token AccessToken
     * @return User ID
     */
    public Long getUserId(String token) {

        return parseClaims(token).get("userId", Long.class);
    }

    /**
     * JWT 검증
     * @param token JWT
     * @return isValidate
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JWT Claims 추출
     * 만료·위조·형식 오류를 모두 예외로 던진다. (만료 토큰의 claims를 조용히 반환하지 않음)
     * @param accessToken accessToken
     * @return JWT Claims
     */
    public Claims parseClaims(String accessToken) {

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY) // 서명 검증용 비밀키 생성
                    .build()
                    .parseClaimsJws(accessToken) // JWT 파싱 + 서명 검증
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtHandler(ResponseCode.JWT_EXPIRED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new JwtHandler(ResponseCode.JWT_MALFORMED_TOKEN); // 위조된 토큰
        } catch (UnsupportedJwtException e) {
            throw new JwtHandler(ResponseCode.JWT_UNSUPPORTED_TOKEN); // 지원하지 않는 토큰
        } catch (Exception e) {
            throw new JwtHandler(ResponseCode.JWT_INVALID_TOKEN);
        }
    }

    // familyId == sessionId
    public String generateFamilyId() {

        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 바이트 배열을 JWT 규격에 맞는 URL-safe Base64 문자열로 변환하는 코드
    }

    public String generateRefreshToken() {

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String generateJTI() {

        return UUID.randomUUID().toString();
    }

    /**
     * Redis에 RefreshToken 원문이 아니라, SHA-256 해시값만 저장하기 위한 해시 변환 메서드
     * @param rawToken Refresh Token 평문
     * @return Refresh Token을 해시로 변환
     */
    public String generateSHA256Token(String rawToken) {

        try {
            // SHA-256 해시 알고리즘 객체를 생성
            // 입력 길이와 상관없이 항상 32바이트 결과를 출력
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(rawToken.getBytes(StandardCharsets.UTF_8)); // 문자열을 바이트 배열로 변환, 해시 함수는 바이트를 입력으로 받음.
            return Base64.getUrlEncoder().withoutPadding().encodeToString(dig); // 바이트 배열을 JWT 규격에 맞는 URL-safe Base64 문자열로 변환하는 코드
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
