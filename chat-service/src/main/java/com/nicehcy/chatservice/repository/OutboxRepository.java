package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}
