package com.nicehcy2.repository;

import com.nicehcy2.entity.UserOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOutboxRepository extends JpaRepository<UserOutbox, Long> {
}
