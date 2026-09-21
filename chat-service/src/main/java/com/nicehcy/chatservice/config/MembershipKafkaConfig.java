package com.nicehcy.chatservice.config;

import com.nicehcy.chatservice.dto.MembershipEventDto;
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
public class MembershipKafkaConfig {

    /**
     * 멤버십 이벤트 전용 리스너 컨테이너 팩토리. 역직렬화 타입만 갈아끼운다.
     * 새 노드가 뜰 때 과거 이벤트를 재생하면 시스템 메시지가 중복 저장되므로 latest로 시작한다.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MembershipEventDto> membershipListenerContainerFactory(
            final KafkaProperties kafkaProperties) {

        final Map<String, Object> consumerProperties = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        consumerProperties.remove(JsonDeserializer.VALUE_DEFAULT_TYPE);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        final JsonDeserializer<MembershipEventDto> valueDeserializer = new JsonDeserializer<>(MembershipEventDto.class);
        valueDeserializer.setUseTypeHeaders(false);

        final ConcurrentKafkaListenerContainerFactory<String, MembershipEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
                consumerProperties, new StringDeserializer(), valueDeserializer, false));

        return factory;
    }
}
