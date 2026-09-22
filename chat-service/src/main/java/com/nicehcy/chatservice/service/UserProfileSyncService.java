package com.nicehcy.chatservice.service;

import com.nicehcy.chatservice.dto.UserProfileEventDto;
import com.nicehcy.chatservice.entity.ChatUserProfile;
import com.nicehcy.chatservice.entity.enums.AgeGroup;
import com.nicehcy.chatservice.entity.enums.JobGroup;
import com.nicehcy.chatservice.repository.ChatUserProfileRepository;
import com.nicehcy.chatservice.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * user-service 프로필 이벤트를 chat_user_profile에 반영한다. 이 서비스가 사본의 유일한 갱신 주체다.
 * upsert라 같은 이벤트를 여러 번 받아도 결과가 같다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileSyncService {

    private final ChatUserProfileRepository chatUserProfileRepository;
    private final FcmTokenRepository fcmTokenRepository;

    public void apply(UserProfileEventDto event) {

        AgeGroup ageGroup = parse(AgeGroup.class, event.ageGroup(), AgeGroup.UNDECIDED);
        JobGroup jobGroup = parse(JobGroup.class, event.jobGroup(), JobGroup.UNDECIDED);

        chatUserProfileRepository.findById(event.userId()).ifPresentOrElse(
                profile -> profile.apply(event.nickname(), event.imageUrl(), ageGroup, jobGroup, event.active()),
                () -> chatUserProfileRepository.save(ChatUserProfile.builder()
                        .userId(event.userId())
                        .nickname(event.nickname())
                        .imageUrl(event.imageUrl())
                        .ageGroup(ageGroup)
                        .jobGroup(jobGroup)
                        .active(event.active())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        // 탈퇴 유저에게 푸시가 가지 않도록. users FK의 cascade를 이벤트가 대신한다
        if (!event.active()) {
            fcmTokenRepository.deleteByUserUserId(event.userId());
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 {} 값 '{}' → {}", type.getSimpleName(), value, fallback);
            return fallback;
        }
    }
}
