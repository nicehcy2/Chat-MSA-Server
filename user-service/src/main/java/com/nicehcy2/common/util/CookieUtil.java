package com.nicehcy2.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

public class CookieUtil {

    public static final Duration REFRESH_TTL = Duration.ofDays(14);

    public static void addAuthCookies(HttpServletResponse response,
                                      String refreshToken,
                                      String sessionId) {
        response.addCookie(createHttpOnlyCookie(
                "refreshToken", refreshToken, REFRESH_TTL
        ));
        response.addCookie(createHttpOnlyCookie(
                "sessionId", sessionId, REFRESH_TTL
        ));
    }

    private static Cookie createHttpOnlyCookie(String name, String value, Duration maxAge) {

        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(maxAge.getSeconds()));

        return cookie;
    }
}
