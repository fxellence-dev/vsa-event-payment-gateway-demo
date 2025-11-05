package com.vsa.paymentgateway.customer.config;

import org.apache.kafka.clients.producer.Producer;
import org.axonframework.extensions.kafka.eventhandling.DefaultKafkaMessageConverter;
import org.axonframework.extensions.kafka.eventhandling.KafkaMessageConverter;
import org.axonframework.extensions.kafka.eventhandling.producer.ConfirmationMode;
import org.axonframework.extensions.kafka.eventhandling.producer.ProducerFactory;
import org.axonframework.serialization.Serializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Event Bus Configuration for Customer Service (Microservices Mode).
 * 
 * <p>This configuration enables Kafka as the distributed event bus, allowing
 * Customer Service to publish domain events to Kafka topics that other 
 * microservices can consume.</p>
 */
@Configuration
@Profile("microservices")
public class KafkaEventBusConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${axon.kafka.default-topic}")
    private String defaultTopic;
    
    /**
     * Configure Kafka producer factory for Axon
     */
    @Bean
    public ProducerFactory<String, byte[]> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                   org.apache.kafka.common.serialization.StringSerializer.class);
        config.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                   org.apache.kafka.common.serialization.ByteArraySerializer.class);
        config.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        config.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, 3);
        config.put(org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        org.springframework.kafka.core.ProducerFactory<String, byte[]> springFactory = 
            new DefaultKafkaProducerFactory<>(config);
            
        // Return Axon producer factory
        return new ProducerFactory<String, byte[]>() {
            @Override
            public Producer<String, byte[]> createProducer() {
                return springFactory.createProducer();
            }
            
            @Override
            public ConfirmationMode confirmationMode() {
                return ConfirmationMode.WAIT_FOR_ACK;
            }
            
            @Override
            public void shutDown() {
                // Spring manages lifecycle
            }
        };
    }
    
    /**
     * Configure message converter for Kafka events
     */
    @Bean
    public KafkaMessageConverter<String, byte[]> kafkaMessageConverter(Serializer serializer) {
        return DefaultKafkaMessageConverter.builder()
                .serializer(serializer)
                .build();
    }
    
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
}

