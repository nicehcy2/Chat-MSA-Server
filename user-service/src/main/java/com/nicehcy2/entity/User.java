package com.nicehcy2.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Column(name = "profile_url")
    private String imageUrl;

    // TODO: ROLE 만들자
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    private UserRole userRole;
}
