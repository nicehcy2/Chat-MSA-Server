package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.ChatRoomMembership;

public enum MembershipStatus {
    NONE, JOINED, LEFT, BANNED;

    // join API의 멤버십 분기와 같은 기준. 강퇴는 leftAt과 무관하게 최우선
    public static MembershipStatus from(ChatRoomMembership membership) {
        if (Boolean.TRUE.equals(membership.getIsBanned())) {
            return BANNED;
        }
        return membership.getLeftAt() == null ? JOINED : LEFT;
    }
}
