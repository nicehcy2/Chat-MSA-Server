package com.nicehcy.chatservice.config;

import com.nicehcy.chatservice.dto.ReadReceiptEventDto;
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
public class ReadReceiptKafkaConfig {

    /**
     * 읽음 이벤트 전용 리스너 컨테이너 팩토리.
     *
     * 전역 consumer 설정(spring.json.value.default.type)에 MessageResponseDto가 박혀 있어서,
     * 기본 팩토리를 그대로 쓰면 읽음 이벤트까지 MessageResponseDto로 역직렬화되어 깨진다.
     * bootstrap-servers 같은 공통 설정은 그대로 물려받고 역직렬화 타입만 갈아끼운다.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReadReceiptEventDto> readReceiptListenerContainerFactory(
            final KafkaProperties kafkaProperties) {

        final Map<String, Object> consumerProperties = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        consumerProperties.remove(JsonDeserializer.VALUE_DEFAULT_TYPE);

        // 전역 설정은 earliest지만 읽음 이벤트는 latest로 덮는다.
        // groupId가 노드마다 다르므로 새 노드가 처음 뜰 때 earliest면 토픽에 쌓인 과거 읽음 이벤트를
        // 전부 재생해 구독자에게 쏟아붓는다. 워터마크라 결과는 같지만 순수한 낭비다.
        // (재시작 시에는 커밋된 오프셋부터 읽으므로 이 설정과 무관하다)
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        final JsonDeserializer<ReadReceiptEventDto> valueDeserializer = new JsonDeserializer<>(ReadReceiptEventDto.class);
        valueDeserializer.setUseTypeHeaders(false); // 프로듀서가 타입 헤더를 붙이지 않으므로 타입은 위에서 못박은 값을 쓴다

        final ConcurrentKafkaListenerContainerFactory<String, ReadReceiptEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        // configureDeserializers = false: 설정 맵이 위에서 지정한 역직렬화 타입을 덮어쓰지 못하게 한다.
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
                consumerProperties, new StringDeserializer(), valueDeserializer, false));

        return factory;
    }
}
