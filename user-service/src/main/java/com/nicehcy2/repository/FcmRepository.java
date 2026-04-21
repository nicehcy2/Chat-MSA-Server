package com.nicehcy2.repository;

import com.nicehcy2.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FcmRepository extends JpaRepository<FcmToken, Long> {
}
