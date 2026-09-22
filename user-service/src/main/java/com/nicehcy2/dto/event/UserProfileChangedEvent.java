package com.nicehcy2.dto.event;

import com.nicehcy2.entity.User;
import com.nicehcy2.entity.enums.AgeGroup;
import com.nicehcy2.entity.enums.JobGroup;

/**
 * 채팅 쪽 프로필 사본(chat_user_profile)의 계약. 필드는 채팅이 필요한 것만 싣는다.
 * 이 record가 바뀌면 chat-service의 컨슈머 DTO도 같이 바뀌어야 한다.
 */
public record UserProfileChangedEvent(
        Long userId,
        String nickname,
        String imageUrl,
        AgeGroup ageGroup,
        JobGroup jobGroup,
        boolean active
) {

    public static UserProfileChangedEvent from(User user) {
        return new UserProfileChangedEvent(
                user.getUserId(), user.getNickname(), user.getImageUrl(),
                user.getAgeGroup(), user.getJobGroup(), user.isStatus());
    }
}
