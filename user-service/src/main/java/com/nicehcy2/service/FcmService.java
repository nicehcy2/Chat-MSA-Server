package com.nicehcy2.service;

import com.nicehcy2.dto.FcmTokenRequestDto;
import com.nicehcy2.entity.FcmToken;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.FcmRepository;
import com.nicehcy2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmRepository fcmRepository;
    private final UserRepository userRepository;

    public Long saveFcmToken(final FcmTokenRequestDto fcmTokenRequestDto) {

        User user = userRepository
                .findById(fcmTokenRequestDto.userId())
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));

        FcmToken fcmToken = FcmToken.builder()
                .user(user)
                .deviceType(fcmTokenRequestDto.deviceType())
                .token(fcmTokenRequestDto.fcmToken())
                .build();

        fcmRepository.save(fcmToken);

        return fcmToken.getId();
    }
}
