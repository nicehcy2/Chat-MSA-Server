package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {
}
