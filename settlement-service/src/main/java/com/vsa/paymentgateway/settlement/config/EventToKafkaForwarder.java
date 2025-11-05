package com.vsa.paymentgateway.settlement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Simple event handler that forwards all domain events to Kafka
 */
@Component
public class EventToKafkaForwarder {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    
    public EventToKafkaForwarder(KafkaTemplate<String, String> kafkaTemplate,
                                  @Value("${axon.kafka.default-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.objectMapper = new ObjectMapper();
        // Register module to handle Java 8 date/time types
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
    
    /**
     * Catch-all event handler that forwards any event to Kafka
     * Filters out Axon framework internal events
     */
    @EventHandler
    public void on(Object event) {
        // Skip Axon framework internal events
        String className = event.getClass().getName();
        if (className.startsWith("org.axonframework.") || 
            className.contains("UnknownSerializedType")) {
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(event);
            System.out.println("Publishing event to Kafka: " + event.getClass().getSimpleName());
            kafkaTemplate.send(topic, event.getClass().getSimpleName(), json);
        } catch (Exception e) {
            System.err.println("Failed to forward event to Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
