package com.nicehcy2.chatapiservice.entity;

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

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "profile_url")
    private String imageUrl;
}