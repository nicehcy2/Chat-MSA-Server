package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.entity.ChatUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatUserProfileRepository extends JpaRepository<ChatUserProfile, Long> {

    // 요청자 검증. 탈퇴(active=false) 유저는 존재하지 않는 것으로 본다
    boolean existsByUserIdAndActiveTrue(Long userId);
}
