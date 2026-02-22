package com.nicehcy.chatservice.config.socket;

import com.nicehcy.chatservice.common.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketSessionInterceptor implements ChannelInterceptor {

    private final SocketConnectionTracker socketConnectionTracker;
    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // STOMP 헤더 정보를 래핑합니다.
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // STOMP 명령어에 따라 처리합니다.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalStateException("Unauthorized");
            }

            String token = authHeader.substring(7);
            if (!jwtProvider.validate(token)) {
                throw new IllegalStateException("Invalid token");
            }

            Claims claims = jwtProvider.parseClaims(token);
            String userId = String.valueOf(claims.get("userId"));

            // sessionAttributes에 userId 저장
            accessor.getSessionAttributes().put("userId", userId);
            socketConnectionTracker.setUserOnline(Long.parseLong(userId));

            log.info("User {} is now ONLINE.", userId);
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
                Long userId = Long.parseLong((String) sessionAttributes.get("userId"));
                socketConnectionTracker.setUserOffline(userId);

                log.info("User {} is now OFFLINE.", userId);
            }
        }

        return message;
    }
}