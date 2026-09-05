package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 정원 판정과 증가를 UPDATE 한 문장으로. 영향 행 0 = 자리 없음.
    // clearAutomatically는 붙이지 않는다: 같은 트랜잭션에서 먼저 조회한 room/membership을 이어서 쓴다
    @Modifying
    @Query("UPDATE ChatRoom r SET r.participationCount = r.participationCount + 1 " +
           "WHERE r.id = :chatRoomId AND r.participationCount < r.maxParticipants")
    int incrementParticipationCountIfNotFull(Long chatRoomId);
}
