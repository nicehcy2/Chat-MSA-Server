package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.common.error.GeneralException;
import com.nicehcy2.chatapiservice.common.error.ResponseCode;
import com.nicehcy2.chatapiservice.dto.FcmTokenRequestDto;
import com.nicehcy2.chatapiservice.entity.FcmToken;
import com.nicehcy2.chatapiservice.repository.ChatUserProfileRepository;
import com.nicehcy2.chatapiservice.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final ChatUserProfileRepository chatUserProfileRepository;

    /**
     * 로그인·새로고침마다 호출되므로 이미 있는 토큰이면 소유자와 기기만 갱신한다.
     * 매번 INSERT하면 같은 기기에 푸시가 중복 발송된다.
     */
    @Transactional
    public Long register(Long requesterId, FcmTokenRequestDto request) {

        if (!chatUserProfileRepository.existsByUserIdAndActiveTrue(requesterId)) {
            throw new GeneralException(ResponseCode.USER_NOT_FOUND);
        }

        return fcmTokenRepository.findByToken(request.fcmToken())
                .map(existing -> {
                    existing.reassign(requesterId, request.deviceType());
                    return existing.getId();
                })
                .orElseGet(() -> fcmTokenRepository.save(FcmToken.builder()
                        .userId(requesterId)
                        .token(request.fcmToken())
                        .deviceType(request.deviceType())
                        .build()).getId());
    }

    @Transactional
    public void delete(Long requesterId, String token) {

        FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                .orElseThrow(() -> new GeneralException(ResponseCode.FCM_TOKEN_NOT_FOUND));
        if (!fcmToken.getUserId().equals(requesterId)) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }
        fcmTokenRepository.delete(fcmToken);
    }
}
