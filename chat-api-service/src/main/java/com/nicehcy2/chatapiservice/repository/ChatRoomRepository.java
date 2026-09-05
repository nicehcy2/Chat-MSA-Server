package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 정원 판정과 증가를 UPDATE 한 문장으로. 영향 행 0 = 자리 없음.
    // clearAutomatically는 붙이지 않는다: 같은 트랜잭션에서 먼저 조회한 room/membership을 이어서 쓴다
    @Modifying
    @Query("UPDATE ChatRoom r SET r.participationCount = r.participationCount + 1 " +
           "WHERE r.id = :chatRoomId AND r.participationCount < r.maxParticipants")
    int incrementParticipationCountIfNotFull(Long chatRoomId);

    // null인 조건은 생략. 대상 Set이 빈 방은 "전체 대상"이라 필터에 항상 포함된다.
    // q는 서비스가 %·_·!를 '!'로 이스케이프한 값이다
    @Query("""
            SELECT r FROM ChatRoom r
            WHERE (:q IS NULL
                   OR r.title LIKE CONCAT('%', :q, '%') ESCAPE '!'
                   OR r.description LIKE CONCAT('%', :q, '%') ESCAPE '!')
              AND (:ageGroup IS NULL OR :ageGroup MEMBER OF r.ageGroups OR r.ageGroups IS EMPTY)
              AND (:jobGroup IS NULL OR :jobGroup MEMBER OF r.jobGroups OR r.jobGroups IS EMPTY)
              AND (:before IS NULL OR r.id < :before)
            ORDER BY r.id DESC
            """)
    List<ChatRoom> findForExplore(String q, AgeGroup ageGroup, JobGroup jobGroup, Long before, Pageable pageable);
}
