package com.nicehcy2.service;

import com.nicehcy2.common.error.ResponseCode;
import com.nicehcy2.common.error.exception.FcmHandler;
import com.nicehcy2.common.error.exception.UserHandler;
import com.nicehcy2.dto.FcmTokenRequestDto;
import com.nicehcy2.entity.FcmToken;
import com.nicehcy2.entity.User;
import com.nicehcy2.repository.FcmRepository;
import com.nicehcy2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmRepository fcmRepository;
    private final UserRepository userRepository;

    /**
     * FCM 토큰을 등록합니다.
     * 클라이언트가 로그인/새로고침마다 호출하므로, 이미 등록된 토큰이면 새로 만들지 않고
     * 소유자·기기 정보만 갱신합니다. 매번 INSERT 하면 같은 기기에 푸시가 중복 발송됩니다.
     */
    @Transactional
    public Long saveFcmToken(final FcmTokenRequestDto fcmTokenRequestDto) {

        User user = userRepository
                .findById(fcmTokenRequestDto.userId())
                .orElseThrow(() -> new UserHandler(ResponseCode.USER_NOT_FOUND));

        // Redis에도 저장

        return fcmRepository.findByToken(fcmTokenRequestDto.fcmToken())
                .map(existingToken -> {
                    existingToken.reassign(user, fcmTokenRequestDto.deviceType());
                    log.info("이미 등록된 FCM 토큰 - 갱신 처리 [tokenId: {}]", existingToken.getId());
                    return existingToken.getId();
                })
                .orElseGet(() -> {
                    FcmToken fcmToken = FcmToken.builder()
                            .user(user)
                            .deviceType(fcmTokenRequestDto.deviceType())
                            .token(fcmTokenRequestDto.fcmToken())
                            .build();
                    return fcmRepository.save(fcmToken).getId();
                });
    }

    public void deleteFcmToken(Long fcmTokenId) {

        FcmToken fcmToken = fcmRepository.findById(fcmTokenId)
                .orElseThrow(() -> new FcmHandler(ResponseCode.FCM_TOKEN_NOT_FOUND));

        // Redis에서도 삭제

        fcmRepository.delete(fcmToken);
    }
}
