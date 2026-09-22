package com.nicehcy.chatservice.repository;

import com.nicehcy.chatservice.entity.ChatUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatUserProfileRepository extends JpaRepository<ChatUserProfile, Long> {
}
