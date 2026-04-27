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

        // Redis에도 저장

        FcmToken savedFcmToken = fcmRepository.save(fcmToken);

        return savedFcmToken.getId();
    }

    public void deleteFcmToken(Long fcmTokenId) {

        FcmToken fcmToken = fcmRepository.findById(fcmTokenId)
                .orElseThrow(() -> new RuntimeException("해당 ID를 가진 FCM Token이 존재하지 않습니다. " + fcmTokenId));

        // Redis에서도 삭제

        fcmRepository.delete(fcmToken);
    }
}
