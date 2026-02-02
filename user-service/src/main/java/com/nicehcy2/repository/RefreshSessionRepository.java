package com.nicehcy2.repository;

import org.springframework.stereotype.Repository;


@Repository
public class RefreshSessionRepository {

    /*
    private final RedisTemplate<String, RedisSessionDto> redisTemplate;

    private String keySession(String sessionId) {
        return "rt:session:" + sessionId;
    }

    public Optional<RedisSessionDto> findBySessionId(String sessionId) {

        return Optional.ofNullable(redisTemplate.opsForValue().get(keySession(sessionId)));
    }*/
}
