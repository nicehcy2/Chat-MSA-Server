package com.nicehcy2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false)
    private AgeGroup ageGroup;

    @Column(name = "birthday", nullable = false)
    private String birthDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_group", nullable = false)
    private JobGroup jobGroup;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Column(name="reward", nullable = false)
    private int reward;

    @Column(name="status", nullable = false)
    private boolean status;

    @Column(name="day_target_expenditure", nullable = false)
    private int dayTargetExpenditure;

    @Column(name = "inactive_date")
    private LocalDateTime inactiveDate;

    @Column(name = "profile_url")
    private String imageUrl;
}
