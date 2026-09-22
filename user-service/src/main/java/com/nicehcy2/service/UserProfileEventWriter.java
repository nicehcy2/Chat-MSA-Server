package com.nicehcy2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicehcy2.dto.event.UserProfileChangedEvent;
import com.nicehcy2.entity.User;
import com.nicehcy2.entity.UserOutbox;
import com.nicehcy2.repository.UserOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 프로필 변경을 Outbox에 기록한다. 호출자의 트랜잭션 안에서 실행되어 유저 변경과 함께 커밋되거나 함께 롤백된다.
 * 사본은 이벤트가 빠지면 스스로 복구되지 않으므로 직접 발행 대신 Outbox를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class UserProfileEventWriter {

    // Debezium이 aggregate_type으로 토픽을 정한다: {aggregateType}-topic
    static final String AGGREGATE_TYPE = "user-profile";
    static final String EVENT_TYPE = "PROFILE_CHANGED";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UserOutboxRepository userOutboxRepository;

    public void profileChanged(User user) {
        UserProfileChangedEvent event = UserProfileChangedEvent.from(user);
        userOutboxRepository.save(new UserOutbox(
                AGGREGATE_TYPE, String.valueOf(user.getUserId()), EVENT_TYPE, toJson(event)));
    }

    private static String toJson(UserProfileChangedEvent event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("프로필 이벤트 직렬화 실패", e);
        }
    }
}
