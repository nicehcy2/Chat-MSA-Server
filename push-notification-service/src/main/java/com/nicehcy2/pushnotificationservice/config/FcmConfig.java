package com.nicehcy2.pushnotificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK 초기화 설정 클래스
 *
 * Firebase Admin SDK는 서버에서 FCM 푸시 알림을 전송하기 위한 라이브러리입니다.
 * 서비스 계정 JSON 파일을 통해 Firebase 서버와 인증하며,
 * 초기화 이후 FirebaseMessaging.getInstance()로 어디서든 사용 가능합니다.
 */
@Slf4j
@Configuration
public class FcmConfig {

    /**
     * Firebase Admin SDK 초기화 메서드
     *
     * @PostConstruct를 사용하는 이유:
     * FirebaseApp은 Firebase 내부에서 자체적으로 싱글톤 관리를 합니다.
     * 따라서 Spring 빈으로 등록할 필요 없이, Spring 빈 초기화 이후
     * 딱 한 번만 Firebase 내부 싱글톤을 초기화하는 트리거 역할만 합니다.
     *
     * @throws IOException 서비스 계정 파일을 읽지 못할 경우 발생
     */
    @PostConstruct
    public void initFirebase() throws IOException {

        // Firebase 내부 싱글톤에 이미 초기화된 앱이 있으면 스킵
        // 멀티 모듈 또는 테스트 환경에서 중복 초기화 방지
        if (FirebaseApp.getApps().isEmpty()) {

            // classpath(src/main/resources/)에서 서비스 계정 JSON 파일 로드
            // Firebase Console > 프로젝트 설정 > 서비스 계정 > 새 비공개 키 생성에서 발급
            // 보안상 절대 Git에 커밋하면 안됨 (.gitignore 등록 필수)
            InputStream serviceAccount =
                    getClass().getClassLoader()
                            .getResourceAsStream("firebase-service-account.json");

            if (serviceAccount == null) {
                log.error("firebase-service-account.json 파일을 찾을 수 없습니다.");
                return;
            }

            // 서비스 계정 JSON으로 Google 인증 정보를 생성하고 Firebase 옵션에 설정
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Firebase 내부 싱글톤에 앱 등록
            // 이후 FcmService에서 FirebaseMessaging.getInstance()로 바로 사용 가능
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK 초기화 완료");
        }
    }
}