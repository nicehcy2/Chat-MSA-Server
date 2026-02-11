package com.nicehcy2.gatewayserver.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtProvider {

    private final Key key;

    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     *
     * @param token Jwt 토큰
     * @return JWT를 검증하고 Payload를 꺼내서 Claims 객체로 반환
     */
    public Claims parseClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key) // 키가 맞지 않으면 예외 발생. 서명 검증에 사용할 비밀키 설정
                .build()
                .parseClaimsJws(token) // 토큰 파싱과 동시에 서명 검증 + 만료 검사 + 형식 검사
                .getBody(); // Payload를 꺼냄
    }

    public boolean validate(String token) {
        try {
            parseClaims(token); // 파싱 + 검증
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
