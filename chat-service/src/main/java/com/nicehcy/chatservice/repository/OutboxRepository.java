package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    Optional<Outbox> findByAggregateId(String aggregateId);
}
