package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.dto.ChatServerInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatApiService {

    private final DiscoveryClient discoveryClient;

    public ChatServerInfoResponse assignChatServer() {

        // 적절한 채팅 서버 가져오기
        return getServerInstance();
    }

    private ChatServerInfoResponse getServerInstance() {

        List<ServiceInstance> instances = discoveryClient.getInstances("chat-service");

        if (instances.size() == 0) {
            return null;
        }
        // TODO: get(0) 로직 말고 서버를 선택하는 조건을 추가해줘야 한다.
        String nodeId = instances.get(0).getInstanceId();
        String serviceUri = String.valueOf(instances.get(0).getUri());

        return ChatServerInfoResponse.builder()
                .nodeId(nodeId)
                .websocketUrl(serviceUri)
                .build();
    }

    /*
    public Assigned assign(String userId, @Nullable String roomId) {
        var instances = discoveryClient.getInstances("chat-service");
        if (instances.isEmpty()) throw new IllegalStateException("활성 채팅 서버 없음");

        ServiceInstance chosen = choose(instances, roomId);

        String nodeId = chosen.getInstanceId();                    // = CHAT_NODE_ID
        String wsUrl = chosen.getMetadata().get("wsUrl");

        // (선택) 관측을 위해 매핑 저장
        redis.opsForValue().set("chat:user:"+userId+":node", nodeId, 60, TimeUnit.MINUTES);
        redis.opsForHash().put("chat:nodes:meta", nodeId, wsUrl);

        String token = issueConnectToken(userId, nodeId);
        return new Assigned(nodeId, wsUrl, token);
    }

    private ServiceInstance choose(List<ServiceInstance> list, String roomId) {
        if (roomId != null && !roomId.isBlank()) {
            // 방 단위 일관 라우팅: Rendezvous Hash
            return list.stream()
                .max(Comparator.comparingLong(si -> rendezvous(roomId, si.getInstanceId())))
                .orElse(list.get(0));
        }
        // 기본: 같은 리전 우선 + (load/capacity) 낮은 순
        return list.stream().sorted(
            Comparator
              .comparing((ServiceInstance si) -> !myRegion.equals(si.getMetadata().get("region")))
              .thenComparingDouble(this::relativeLoad)
        ).findFirst().get();
    }

    private double relativeLoad(ServiceInstance si) {
        String nodeId = si.getInstanceId();
        long load = Optional.ofNullable(redis.opsForValue().get("chat:nodes:"+nodeId+":load"))
                            .map(Long::parseLong).orElse(0L);
        long cap  = Optional.ofNullable(si.getMetadata().get("capacity"))
                            .map(Long::parseLong).orElse(10_000L);
        return (double) load / Math.max(1, cap);
    }

    private long rendezvous(String key, String nodeId) {
        return java.util.Objects.hash(key, nodeId); // 실제론 Murmur3/xxHash 권장
    }

    private String issueConnectToken(String userId, String nodeId) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))   // 1분 권장
                .claim("nodeId", nodeId)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

     */
}
