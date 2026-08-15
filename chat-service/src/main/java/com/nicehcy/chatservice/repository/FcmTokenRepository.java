package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // 여러 유저 ID로 토큰 한 번에 조회
    List<FcmToken> findByUserUserIdIn (List<Long> userIds);
}
