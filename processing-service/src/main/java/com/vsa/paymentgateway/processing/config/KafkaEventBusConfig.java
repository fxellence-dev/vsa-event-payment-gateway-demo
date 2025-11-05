package com.vsa.paymentgateway.processing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Event Bus Configuration for Processing Service (Microservices Mode).
 * 
 * <p>This configuration is activated only when the 'microservices' profile is active.
 * It enables Kafka as the distributed event bus through the Axon Kafka Spring Boot Starter,
 * allowing Processing Service to publish payment processing outcomes to Kafka topics that
 * other microservices can consume.</p>
 * 
 * <h3>Auto-Configuration:</h3>
 * <p>The Axon Kafka Spring Boot Starter automatically configures:</p>
 * <ul>
 *   <li><b>KafkaPublisher</b>: Publishes events to Kafka topics</li>
 *   <li><b>KafkaMessageConverter</b>: Converts Axon domain events to Kafka messages</li>
 *   <li><b>ProducerFactory</b>: Creates Kafka producers with reliability settings</li>
 * </ul>
 * 
 * <h3>Event Flow:</h3>
 * <pre>
 * Processing Service → Kafka Topic (payment-events) → Other Services
 * 
 * Published Events:
 * - PaymentProcessedEvent → Consumed by: Orchestration Service (continue saga)
 * - PaymentProcessingFailedEvent → Consumed by: Orchestration Service (compensation)
 * - PaymentRefundedEvent → Consumed by: Gateway API (read models)
 * </pre>
 * 
 * <h3>Processing Logic:</h3>
 * <ul>
 *   <li>Simulates external payment processor (Stripe/Adyen)</li>
 *   <li>90% success rate (simulated)</li>
 *   <li>Failure scenarios: Insufficient funds, fraud, expired card, etc.</li>
 *   <li>Generates processor transaction IDs</li>
 * </ul>
 * 
 * <h3>Configuration (application.yml):</h3>
 * <pre>
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *     producer:
 *       key-serializer: org.apache.kafka.common.serialization.StringSerializer
 *       value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
 *       acks: all
 *       retries: 3
 * 
 * axon:
 *   kafka:
 *     client-id: processing-service
 *     default-topic: payment-events
 *     publisher:
 *       confirmation-mode: transactional
 * </pre>
 * 
 * <h3>Deployment Notes:</h3>
 * <ul>
 *   <li><b>Monolith Mode</b>: This config is inactive; uses in-memory event bus</li>
 *   <li><b>Microservices Mode</b>: This config is active; uses Kafka event bus</li>
 *   <li><b>Reliability</b>: Transactional mode with acks=all for guaranteed delivery</li>
 * </ul>
 * 
 * @see org.axonframework.extensions.kafka.autoconfig.KafkaAutoConfiguration
 * @see org.axonframework.extensions.kafka.eventhandling.KafkaPublisher
 * @author VSA Team
 * @since 1.0.0
 */
@Configuration
@Profile("microservices")
public class KafkaEventBusConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    /**
     * Create simple KafkaTemplate for publishing events as JSON strings
     */
    @Bean
    public org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> config = new HashMap<>();
        config.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                   org.apache.kafka.common.serialization.StringSerializer.class);
        config.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                   org.apache.kafka.common.serialization.StringSerializer.class);
        config.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        config.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, 3);
        
        org.springframework.kafka.core.ProducerFactory<String, String> pf = 
            new DefaultKafkaProducerFactory<>(config);
            
        return new org.springframework.kafka.core.KafkaTemplate<>(pf);
    }
    
    /**
     * This class relies on Axon Kafka Spring Boot Starter auto-configuration.
     * 
     * <p>The axon-kafka-spring-boot-starter dependency automatically creates all
     * necessary beans for Kafka event publishing based on the configuration in
     * application.yml (spring.kafka.* and axon.kafka.* properties).</p>
     * 
     * <p>No explicit bean definitions are required unless you need to customize
     * the default behavior (e.g., custom serializers, topic routing, etc.).</p>
     */
}
