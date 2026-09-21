package com.nicehcy.chatservice.config.socket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이 노드에 붙어 있는 WebSocket 세션을 id로 찾아 서버 쪽에서 닫기 위한 레지스트리.
 * STOMP 세션 id는 WebSocketSession id와 같으므로 Redis 온라인 키의 세션 id로 바로 조회할 수 있다.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry implements WebSocketHandlerDecoratorFactory {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessions.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    // 닫힌 세션의 Redis 정리는 이후 발생하는 DISCONNECT 처리(SocketSessionInterceptor)가 맡는다
    public void close(Collection<String> sessionIds, CloseStatus status) {
        for (String sessionId : sessionIds) {
            WebSocketSession session = sessions.get(sessionId);
            if (session == null || !session.isOpen()) continue;
            try {
                session.close(status);
            } catch (IOException e) {
                log.warn("세션 종료 실패 [sessionId: {}]: {}", sessionId, e.getMessage());
            }
        }
    }
}
