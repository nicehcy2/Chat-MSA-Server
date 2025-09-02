package com.nicehcy.chatservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // 웹 소켓 통신이 /ws으로 도착할 때, 해당 통신이 웹 소켓 통신 중 stomp 통신인 것을 확인하고, 이를 연결.
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
                // .withSockJS(); // Jmeter 테스트 시 주석 제거
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        /**
         * Kafka (외부브로커) 사용
         */
        registry.setPathMatcher(new AntPathMatcher(".")); // URL을 / -> .으로
        registry.setApplicationDestinationPrefixes("/pub");  //  @MessageMapping 메서드로 라우팅된다.  Client에서 SEND 요청을 처리
        registry.enableSimpleBroker("/sub"); // /sub/{chatNo} 로 주제 구독 가능
    }
}
