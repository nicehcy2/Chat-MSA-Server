package com.nicehcy.chatservice.config.socket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SocketConnectionTracker {

    @Value("${ONLINE_KEY_PREFIX}")
    private String ONLINE_KEY_PREFIX;

    private final RedisTemplate<String, Object> redisTemplate;
    // TODO: 추후에 사용자의 동작을 감지해서 RefreshToken 요청을 정확하게 해줄 수 있다면 사용자 온라인 TTL을 accessToken가 일치시키자.
    private final long TTL_MINUTES = 30; // 사용자 온라인 TTL 시간 - 소켓 비정상연결 종료 관리위함

    /**
     * 사용자 ID를 기반으로 온라인 상태를 Redis에 기록합니다.
     * 필요에 따라 TTL(Time To Live)을 설정할 수도 있습니다.
     *
     * @param userId 연결된 사용자의 고유 ID
     */
    public void setUserOnline(Long userId) {
        final String key = ONLINE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, true, TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 사용자 ID에 해당하는 온라인 상태 키를 삭제하여 오프라인 상태로 만듭니다.
     *
     * @param userId 연결 종료한 사용자의 고유 ID
     */
    public void setUserOffline(Long userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
    }

    /**
     * 특정 사용자 ID의 온라인 여부를 반환합니다.
     *
     * @param userId 확인할 사용자의 고유 ID
     * @return 온라인이면 true, 그렇지 않으면 false
     */
    public boolean isUserOnline(Long userId) {
        Object status = redisTemplate.opsForValue().get(ONLINE_KEY_PREFIX + userId);
        return status != null && (Boolean) status;
    }

    public void refreshOnline(Long userId) {

        String key = ONLINE_KEY_PREFIX + userId;
        redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
        // 키가 없을 수도 있으니 안전하게: 없으면 새로 셋
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            setUserOnline(userId);
        }
    }
}
