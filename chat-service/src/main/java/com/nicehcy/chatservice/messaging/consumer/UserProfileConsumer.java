package com.nicehcy.chatservice.messaging.consumer;

import com.nicehcy.chatservice.dto.UserProfileEventDto;
import com.nicehcy.chatservice.service.UserProfileSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileConsumer {

    private final UserProfileSyncService userProfileSyncService;

    /**
     * DB 쓰기라 한 노드만 처리하면 되므로 푸시 컨슈머처럼 모든 노드가 공유하는 groupId를 쓴다.
     * 파티션 키가 userId라 같은 유저의 변경 순서는 유지된다.
     */
    @KafkaListener(
            topics = "${USER_PROFILE_TOPIC:user-profile-topic}",
            groupId = "${USER_PROFILE_GROUP_ID:chat-user-profile-group}",
            containerFactory = "userProfileListenerContainerFactory")
    public void listen(@Payload final UserProfileEventDto event) {

        userProfileSyncService.apply(event);
        log.info("프로필 사본 반영 [userId: {}, active: {}]", event.userId(), event.active());
    }
}
