package com.nicehcy2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicehcy2.entity.User;
import com.nicehcy2.entity.UserOutbox;
import com.nicehcy2.entity.enums.AgeGroup;
import com.nicehcy2.entity.enums.JobGroup;
import com.nicehcy2.entity.enums.UserRole;
import com.nicehcy2.repository.UserOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * Outbox 페이로드는 chat-service 컨슈머가 필드명으로 역직렬화한다.
 * 이름이 어긋나면 컴파일도 테스트도 통과한 채 운영에서 컨슈머가 죽으므로 계약을 여기서 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileEventWriterTest {

    @Mock UserOutboxRepository userOutboxRepository;

    @InjectMocks UserProfileEventWriter writer;

    @Test
    void 프로필_변경은_유저_id를_키로_채팅이_필요한_필드만_담아_Outbox에_남긴다() throws Exception {
        User user = User.builder()
                .userId(7L).nickname("티끌모아태산").imageUrl("https://img/7.png")
                .ageGroup(AgeGroup.THIRTIES).jobGroup(JobGroup.EMPLOYEE)
                .email("a@b.c").password("*hash*").userRole(UserRole.USER).status(true)
                .build();

        writer.profileChanged(user);

        ArgumentCaptor<UserOutbox> captor = ArgumentCaptor.forClass(UserOutbox.class);
        verify(userOutboxRepository).save(captor.capture());
        UserOutbox outbox = captor.getValue();
        assertEquals("user-profile", outbox.getAggregateType());
        assertEquals("7", outbox.getAggregateId());
        assertEquals("PROFILE_CHANGED", outbox.getEventType());

        JsonNode payload = new ObjectMapper().readTree(outbox.getMessageDtoPayload());
        assertEquals(7L, payload.get("userId").asLong());
        assertEquals("티끌모아태산", payload.get("nickname").asText());
        assertEquals("https://img/7.png", payload.get("imageUrl").asText());
        assertEquals("THIRTIES", payload.get("ageGroup").asText());
        assertEquals("EMPLOYEE", payload.get("jobGroup").asText());
        assertTrue(payload.get("active").asBoolean());

        Set<String> keys = new java.util.HashSet<>();
        payload.fieldNames().forEachRemaining(keys::add);
        assertEquals(Set.of("userId", "nickname", "imageUrl", "ageGroup", "jobGroup", "active"), keys);
    }
}
