package com.nicehcy.chatservice.config.socket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketSessionInterceptor implements ChannelInterceptor {

    private final SocketConnectionTracker socketConnectionTracker;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // STOMP 헤더 정보를 래핑합니다.
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        accessor.setUser(auth);

        // TODO: Authorization 가져오기
        if (accessor.getCommand() == null || accessor.getNativeHeader("x-user-id") == null) {
            return message;
        }

        Long userId = Long.parseLong(accessor.getNativeHeader("x-user-id").get(0));

        // STOMP 명령어에 따라 처리합니다.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 로그인 없는 버전
            socketConnectionTracker.setUserOnline(userId);
            log.info("User {} is now ONLINE.", userId);

            // TODO: 인증 추가
            /*
            if (accessor.getUser() != null) {
                // REST API와 동일하게 FindLoginUser를 통해 사용자 ID를 확인합니다.
                String email = accessor.getUser().getName();
                // Long userId = FindLoginUser.toId(email);
                // socketConnectionTracker.setUserOnline();
                // log.info("User {} is now ONLINE.", userId);
            }
             */
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {

            socketConnectionTracker.setUserOffline(userId);
            log.info("User {} is now ONLINE.", userId);

            /*
            if (accessor.getUser() != null) {
                String email = accessor.getUser().getName();

                // Long userId = FindLoginUser.toId(email);
                // tracker.setUserOffline(userId);
                // log.info("User {} is now OFFLINE.", userId);
            }
             */
        }

        return message;
    }
}