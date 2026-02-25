package com.nicehcy2.service;

import com.nicehcy2.dto.MyPageUserInfoResponseDto;
import com.nicehcy2.dto.UserInfoRequestDto;
import com.nicehcy2.entity.AgeGroup;
import com.nicehcy2.entity.JobGroup;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public MyPageUserInfoResponseDto getUserInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 없습니다."));

        return MyPageUserInfoResponseDto.builder()
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .reward(user.getReward())
                .dayTargetExpenditure(user.getDayTargetExpenditure())
                .jobGroup(user.getJobGroup().getJobGroup())
                .nickname(user.getNickname())
                .ageGroup(user.getAgeGroup().getAgeGroup())
                .userId(user.getUserId())
                .build();
    }

    @Transactional
    public void modifyUserProfile(Long userId, UserInfoRequestDto userInfoRequestDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 없습니다."));

        user.patch(
                userInfoRequestDto.nickname(),
                userInfoRequestDto.gender(),
                AgeGroup.valueOf(userInfoRequestDto.ageGroup()),
                JobGroup.valueOf(userInfoRequestDto.jobGroup()),
                userInfoRequestDto.imageUrl()
        );
    }
}
