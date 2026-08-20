package com.nicehcy2.service;

import com.nicehcy2.common.error.ResponseCode;
import com.nicehcy2.common.error.exception.UserHandler;
import com.nicehcy2.dto.MyPageUserInfoResponseDto;
import com.nicehcy2.dto.UserInfoRequestDto;
import com.nicehcy2.entity.enums.AgeGroup;
import com.nicehcy2.entity.enums.JobGroup;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MyPageUserInfoResponseDto getUserInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(ResponseCode.USER_NOT_FOUND));

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
                .orElseThrow(() -> new UserHandler(ResponseCode.USER_NOT_FOUND));

        user.patch(
                userInfoRequestDto.nickname(),
                userInfoRequestDto.gender(),
                userInfoRequestDto.ageGroup() != null ? AgeGroup.valueOf(userInfoRequestDto.ageGroup()) : null,
                userInfoRequestDto.jobGroup() != null ? JobGroup.valueOf(userInfoRequestDto.jobGroup()) : null,
                userInfoRequestDto.imageUrl()
        );
    }
}
