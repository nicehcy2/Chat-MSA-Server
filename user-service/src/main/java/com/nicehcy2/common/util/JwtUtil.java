package com.nicehcy2.common.util;

import com.nicehcy2.dto.CustomUserInfoDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

@Component
@Slf4j
public class JwtUtil {

    private final Key key; // secretKey를 HMAC 알고리즘에 맞게 저장. 해시 서명용 비밀키
    private final long accessTokenExpTime; // 토큰 만료 시간
    public static final Duration OVERLAP_WINDOW = Duration.ofSeconds(30); // 오버랩 허용 시간

    // 무작위 토큰 생성을 위한 상수
    // Random은 예측이 가능하고 시드가 유추되면 다음 값도 예측 가능하기에 암호학적으로 안전한(예측 불가) SecureRandom 사용
    private static final SecureRandom RANDOM = new SecureRandom();

    public JwtUtil(
        @Value("${jwt.secret}") String secretKey,
        @Value("${jwt.expiration-time}") long accessTokenExpTime
    ){

        byte[] keyBytes = Decoders.BASE64.decode(secretKey); // BASE64로 인코딩된 문자열을 바이트로 되돌림

        // JJWT에서 쓰는 코드인데, byte를 기반으로 HMAC 알고리즘에 맞는 Key 객체 생성
        // HMAC은 비밀키를 이용해서 데이터가 위조되지 않았음을 증명하는 해시 방식. 암호화가 아니다.
        // 비밀키 + 메시지를 섞고 SHA-256 해시 계산을 하고 또 한 번 감싸서 해시 계산 -> 무결성 + 인증을 보장.
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpTime = accessTokenExpTime;
    }

    /**
     * Access Token 생성
     * @param user JWT 페이로드에 추가할 사용자 기본 정보
     * @return Access Token String
     */
    public String createAccessToken(CustomUserInfoDto user) {
        return createToken(user, accessTokenExpTime);
    }

    /**
     * JWT 생성
     * @param user JWT 페이로드에 추가할 사용자 기본 정보
     * @param expireTime Access Token 만료 기간
     * @return JWT String
     */
    private String createToken(CustomUserInfoDto user, long expireTime) {

        // 1. JWT payload를 만드는 코드
        // Claims는 JWT의 payload 영역을 의미. JWT 안에 사용자 정보를 넣는다.
        // 서버가 매번 DB를 조회하지 않고 사용자 정보 및 필요한 정보를 조회 가능
        Claims claims = Jwts.claims();
        claims.put("userId", user.userId());
        claims.put("email", user.email());
        claims.put("role", user.role());
        claims.put("sessionId", user.sessionId());

        Instant now = Instant.now();
        Instant tokenValidity = now.plusSeconds(expireTime);

        return Jwts.builder()
                .setClaims(claims) // 사용자 정보를 JWT에 넣음
                .setIssuedAt(Date.from(now)) // JWT 발급 시간, 토큰 재사용 방지
                .setExpiration(Date.from(tokenValidity)) // JWT 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // SHA-256 알고리즘으로 서명, key = 서버만 알고 있는 비밀 키
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
     * @param token Access Token
     * @return isValidate
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty", e);
        }

        return false;
    }

    /**
     * JWT Claims 추출
     * @param accessToken accessToken
     * @return JWT Claims
     */
    public Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key) // 서명 검증용 비밀키 생성
                    .build()
                    .parseClaimsJws(accessToken) // JWT 파싱 + 서명 검증
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String generateFamilyId() {

        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 바이트 배열을 JWT 규격에 맞는 URL-safe Base64 문자열로 변환하는 코드
    }

    public String generateRefreshToken() {

        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Redis에 RefreshToken 원문이 아니라, SHA-256 해시값만 저장하기 위해 해시 변환 메서드
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
