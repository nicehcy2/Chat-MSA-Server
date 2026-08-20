package com.nicehcy2.entity;

import com.nicehcy2.common.BaseEntity;
import com.nicehcy2.dto.SignupRequestDto;
import com.nicehcy2.entity.enums.AgeGroup;
import com.nicehcy2.entity.enums.JobGroup;
import com.nicehcy2.entity.enums.UserRole;
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
public class User extends BaseEntity {

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

    // 중복 가입 방지는 DB 제약이 최종 방어선 (앱 레벨 existsByEmail 체크는 동시 요청에 뚫림)
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    // BCrypt 해시는 항상 60자 고정
    @Column(name = "password", length = 60, nullable = false)
    private String password;

    @Column(name = "reward", nullable = false)
    private int reward;

    @Column(name = "status", nullable = false)
    private boolean status;

    @Column(name = "day_target_expenditure", nullable = false)
    private int dayTargetExpenditure;

    @Column(name = "inactive_date")
    private LocalDateTime inactiveDate;

    @Column(name = "profile_url")
    private String imageUrl;

    /**
     * 회원가입 전용 정적 팩토리.
     * 권한(userRole)·리워드·상태는 클라이언트 입력을 받지 않고 서버가 초기값을 강제한다.
     */
    public static User of(SignupRequestDto dto, String encodedPassword) {

        return User.builder()
                .nickname(dto.nickname())
                .email(dto.email())
                .password(encodedPassword)
                .userRole(UserRole.USER)
                .gender(dto.gender())
                .ageGroup(dto.ageGroup())
                .birthDay(dto.birthDay())
                .jobGroup(dto.jobGroup())
                .imageUrl(dto.imageUrl())
                .reward(0)
                .status(true)
                .inactiveDate(null)
                .build();
    }

    public void patch(String nickname, String gender, AgeGroup ageGroup, JobGroup jobGroup, String imageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (gender != null) this.gender = gender;
        if (ageGroup != null) this.ageGroup = ageGroup;
        if (jobGroup != null) this.jobGroup = jobGroup;
        if (imageUrl != null) this.imageUrl = imageUrl;
    }
}
