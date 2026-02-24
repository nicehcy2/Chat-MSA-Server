package com.nicehcy2.gatewayserver.filter;

import com.nicehcy2.gatewayserver.common.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthorizationFilter implements GlobalFilter, Ordered {

    private final JwtProvider jwtProvider;

    private static final Set<String> WHITELIST = Set.of(
            "/user-service/login",
            "/user-service/refresh",
            "/user-service/signup",
            "/user-service/signup/email/check"
    );

    /**
     *
     * @param exchange 현재 HTTP 요청 + 응답 + 부가 정보를 담고 있는 컨테이너
     * @param chain 다음 필터로 요청을 전달하기 위한 필터 체인
     * @return Mono<Void> (비동기 처리 결과)
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) return chain.filter(exchange);

        // Authorization에 해당하는 값을 가져옵니다.
        String token = extractToken(exchange);

        // token이 null이거나 검증이 실패하면 401 응답.
        if (token == null || !jwtProvider.validate(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete(); // 더 이상 필터 체인을 진행하지 말고 응답을 여기서 끝냄.
        }

        // Claims(JWT Payload)를 가져온다.
        // 사용자의 정보를 가져온다.
        Claims claims = jwtProvider.parseClaims(token);

        String userId = String.valueOf(claims.get("userId"));
        String email = String.valueOf(claims.get("email"));

        // 다운스트림 서비스로 전달할 헤더 추가
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r
                        .headers(h -> {
                            h.add("X-User-Id", userId);
                            h.add("X-User-Email", email);
                        })).build();

        return chain.filter(mutated);
    }

    private String extractToken(ServerWebExchange exchange) {

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization"); // 요청 헤더의 Authorization를 가져온다.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 접두사 생략
        }
        return null;
    }

    private boolean isWhitelisted(String path) {

        return WHITELIST.contains(path);
    }

    // 필터 실행 순서를 제어
    @Override
    public int getOrder() {
        return -1; // 라우팅 전에 최대한 먼저
    }
}
