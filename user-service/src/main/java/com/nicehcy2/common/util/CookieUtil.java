package com.nicehcy2.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtUtil jwtUtil;

    public void addAuthCookies(HttpServletResponse response,
                               String refreshToken,
                               String sessionId) {
        response.addCookie(createHttpOnlyCookie(
                "refreshToken", refreshToken, jwtUtil.getREFRESH_TTL()
        ));
        response.addCookie(createHttpOnlyCookie(
                "sessionId", sessionId, jwtUtil.getREFRESH_TTL()
        ));
    }

    private Cookie createHttpOnlyCookie(String name, String value, Duration maxAge) {

        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(maxAge.getSeconds()));
        // TODO: 2단계에서 Secure, SameSite 설정 추가

        return cookie;
    }
}
