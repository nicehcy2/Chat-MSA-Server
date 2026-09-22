package com.nicehcy.chatservice.config;

import com.nicehcy.chatservice.dto.UserProfileEventDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class UserProfileKafkaConfig {

    /**
     * 프로필 사본 동기화 전용 리스너 컨테이너 팩토리. 역직렬화 타입만 갈아끼운다.
     * 사본은 upsert라 과거 이벤트를 재생해도 결과가 같으므로 earliest로 시작한다. 새 환경에서 사본을 처음 채울 때도 이 재생에 기댄다.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserProfileEventDto> userProfileListenerContainerFactory(
            final KafkaProperties kafkaProperties) {

        final Map<String, Object> consumerProperties = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        consumerProperties.remove(JsonDeserializer.VALUE_DEFAULT_TYPE);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final JsonDeserializer<UserProfileEventDto> valueDeserializer = new JsonDeserializer<>(UserProfileEventDto.class);
        valueDeserializer.setUseTypeHeaders(false);

        final ConcurrentKafkaListenerContainerFactory<String, UserProfileEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
                consumerProperties, new StringDeserializer(), valueDeserializer, false));

        return factory;
    }
}
