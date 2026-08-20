package com.nicehcy2.common.util;

public final class RedisKeyNamingUtil {

    private static final String RT_SESSION_PREFIX = "rt:session:";

    private RedisKeyNamingUtil() { }

    public static String refreshTokenKey(String familyId) {

        return RT_SESSION_PREFIX + familyId;
    }
}
