package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
