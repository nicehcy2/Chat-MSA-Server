package com.nicehcy.chatservice.config.socket;

import com.nicehcy.chatservice.common.JwtProvider;
import com.nicehcy.chatservice.repository.ChatRoomMembershipRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketSessionInterceptor implements ChannelInterceptor {

    // 허용하는 구독 경로 전체 (default-deny: 여기 매칭되지 않는 구독은 전부 거부)
    // 그룹 1 = chatRoomId, 그룹 2 = ".read" 접미사(읽음 이벤트 토픽) 유무
    private static final Pattern SUBSCRIBE_DESTINATION_PATTERN =
            Pattern.compile("^/sub/chatroom(\\d+)(\\.read)?$");

    private final SocketConnectionTracker socketConnectionTracker;
    private final JwtProvider jwtProvider;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;

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
            socketConnectionTracker.setUserOnline(Long.parseLong(userId), accessor.getSessionId());

            log.info("User {} is now ONLINE. (session: {})", userId, accessor.getSessionId());
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
                Long userId = Long.parseLong((String) sessionAttributes.get("userId"));
                socketConnectionTracker.setUserOffline(userId, accessor.getSessionId());

                log.info("User {} is now OFFLINE. (session: {})", userId, accessor.getSessionId());
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            long userId = Long.parseLong((String) accessor.getSessionAttributes().get("userId"));
            String destination = accessor.getDestination();

            // default-deny: 패턴에 없는 destination은 방 토픽이 아니어도 전부 거부한다.
            // 여기서 던진 예외는 클라이언트에 ERROR 프레임으로 전달되고, STOMP 스펙상 연결이 닫힌다.
            Long chatRoomId = parseChatRoomId(destination);
            if (chatRoomId == null) {
                log.warn("허용되지 않은 구독 경로 거부 [userId: {}, destination: {}]", userId, destination);
                throw new IllegalArgumentException("허용되지 않은 구독 경로입니다: " + destination);
            }

            if (!chatRoomMembershipRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNullAndIsBannedFalse(chatRoomId, userId)) {
                log.warn("비멤버 구독 시도 거부 [userId: {}, chatRoomId: {}]", userId, chatRoomId);
                throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
            }
        }

        return message;
    }

    /**
     * 구독 destination에서 chatRoomId를 추출한다.
     * "/sub/chatroom{id}"(메시지)와 "/sub/chatroom{id}.read"(읽음 이벤트)만 유효하며,
     * 그 외의 경로는 null을 반환해 거부 대상임을 알린다.
     */
    private static Long parseChatRoomId(String destination) {

        if (destination == null) {
            return null;
        }

        Matcher matcher = SUBSCRIBE_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }

        return Long.parseLong(matcher.group(1));
    }
}