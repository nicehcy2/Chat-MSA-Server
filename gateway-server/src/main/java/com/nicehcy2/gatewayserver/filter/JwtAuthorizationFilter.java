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
            "/user-service/refresh"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) return chain.filter(exchange);

        // Authorization에 해당하는 값을 가져옵니다.
        String token = extractToken(exchange);

        if (token == null || !jwtProvider.validate(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Claims claims = jwtProvider.parseClaims(token);

        String userId = String.valueOf(claims.get("userId"));
        String email = String.valueOf(claims.get("email"));

        System.out.println("test: " + userId + " " + email);

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

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        System.out.println("authHeader: " + authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isWhitelisted(String path) {

        if (WHITELIST.contains(path)) return true;
        else return false;
    }

    @Override
    public int getOrder() {
        return -1; // 라우팅 전에 최대한 먼저
    }
}
