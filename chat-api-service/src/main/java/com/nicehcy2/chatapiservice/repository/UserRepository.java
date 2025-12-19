package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
