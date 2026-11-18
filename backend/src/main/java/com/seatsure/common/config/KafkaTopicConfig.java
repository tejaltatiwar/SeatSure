package com.seatsure.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /** Created on startup if missing. Three partitions let consumers scale out; events are keyed by booking id. */
    @Bean
    public NewTopic bookingEventsTopic(AppProperties properties) {
        return TopicBuilder.name(properties.kafka().bookingEventsTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
