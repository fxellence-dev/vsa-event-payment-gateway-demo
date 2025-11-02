package com.vsa.paymentgateway.common.infrastructure;

import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.GenericEventMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Event Gateway implementation using Kafka for cross-bounded context communication
 * Part of the infrastructure layer in VSA
 */
@Component
public class KafkaEventGateway {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventBus eventBus;

    public KafkaEventGateway(KafkaTemplate<String, Object> kafkaTemplate, EventBus eventBus) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventBus = eventBus;
    }

    public void publishEvent(String topic, Object event) {
        kafkaTemplate.send(topic, event);
    }

    public void publishEventLocally(Object event) {
        eventBus.publish(GenericEventMessage.asEventMessage(event));
    }

    public void publishEventToTopicAndLocally(String topic, Object event) {
        publishEvent(topic, event);
        publishEventLocally(event);
    }
}