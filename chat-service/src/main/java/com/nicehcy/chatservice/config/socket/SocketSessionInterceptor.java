package com.nicehcy.chatservice.config.socket;

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
public class SocketSessionInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // STOMP 헤더 정보를 래핑합니다.
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("auth: " + auth);
        accessor.setUser(auth);
        System.out.println("accessor: " + accessor);
        if (accessor.getCommand() == null) {
            return message;
        }

        // STOMP 명령어에 따라 처리합니다.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            if (accessor.getUser() != null) {
                // REST API와 동일하게 FindLoginUser를 통해 사용자 ID를 확인합니다.
                String email = accessor.getUser().getName();
                System.out.println("email: " + email);
                // Long userId = FindLoginUser.toId(email);
                // tracker.setUserOnline(userId);
                // log.info("User {} is now ONLINE.", userId);
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {

            if (accessor.getUser() != null) {
                String email = accessor.getUser().getName();
                System.out.println("email: " + email);

                // Long userId = FindLoginUser.toId(email);
                // tracker.setUserOffline(userId);
                // log.info("User {} is now OFFLINE.", userId);
            }
        }

        return message;
    }
}
