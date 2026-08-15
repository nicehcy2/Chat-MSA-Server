package com.nicehcy.chatservice.config.socket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SocketConnectionTracker {

    // 전역 온라인 키 프리픽스. 노드 구분 없이 socket:online:{userId} Set에 세션ID를 담는다.
    @Value("${ONLINE_KEY_PREFIX:socket:online:}")
    private String ONLINE_KEY_PREFIX;

    private final RedisTemplate<String, String> redisTemplate;
    // TODO: 추후에 사용자의 동작을 감지해서 RefreshToken 요청을 정확하게 해줄 수 있다면 사용자 온라인 TTL을 accessToken과 일치시키자.
    private final long TTL_MINUTES = 30; // 사용자 온라인 TTL 시간 - 소켓 비정상연결 종료 관리위함

    // Set 멤버를 "{nodeId}:{sessionId}" 형태로 저장한다.
    // CONNECT/DISCONNECT는 항상 같은 노드에서 처리되므로 제거 시에도 동일한 멤버 문자열이 만들어진다.
    @Value("${CHAT_NODE_ID}")
    private String nodeId;

    /**
     * 세션 연결을 온라인 상태로 기록합니다.
     * 유저당 하나의 Set에 세션ID를 추가하므로 멀티 디바이스 접속을 지원합니다.
     *
     * @param userId    연결된 사용자의 고유 ID
     * @param sessionId STOMP 세션 ID
     */
    public void setUserOnline(Long userId, String sessionId) {
        final String key = onlineKey(userId);
        redisTemplate.opsForSet().add(key, member(sessionId));
        redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 세션 연결 종료 시 해당 세션만 제거합니다.
     * 다른 디바이스 세션이 남아 있으면 온라인 상태가 유지되고,
     * 마지막 세션이 제거되면 Redis가 빈 Set 키를 자동 삭제해 오프라인이 됩니다.
     *
     * @param userId    연결 종료한 사용자의 고유 ID
     * @param sessionId 종료된 STOMP 세션 ID
     */
    public void setUserOffline(Long userId, String sessionId) {
        redisTemplate.opsForSet().remove(onlineKey(userId), member(sessionId));
    }

    /**
     * 특정 사용자 ID의 온라인 여부를 반환합니다.
     *
     * @param userId 확인할 사용자의 고유 ID
     * @return 온라인이면 true, 그렇지 않으면 false
     */
    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey(userId)));
    }

    /**
     * 여러 사용자 중 오프라인인 사용자만 골라 반환합니다.
     * 온라인 키가 Set 구조라 MGET을 쓸 수 없으므로,
     * 유저별 EXISTS를 pipeline으로 묶어 Redis 왕복 1번에 조회합니다.
     *
     * @param userIds 확인할 사용자 ID 목록
     * @return 오프라인 사용자 ID 목록
     */
    public List<Long> filterOfflineUserIds(final List<Long> userIds) {

        List<Object> exists = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long userId : userIds) {
                connection.keyCommands().exists(onlineKey(userId).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        List<Long> offlines = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            if (!Boolean.TRUE.equals(exists.get(i))) {
                offlines.add(userIds.get(i));
            }
        }
        return offlines;
    }

    private String onlineKey(Long userId) {
        return ONLINE_KEY_PREFIX + userId;
    }

    private String member(String sessionId) {
        return nodeId + ":" + sessionId;
    }
}
