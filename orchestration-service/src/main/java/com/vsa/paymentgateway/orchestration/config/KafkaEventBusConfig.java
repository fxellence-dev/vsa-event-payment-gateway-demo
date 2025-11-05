package com.vsa.paymentgateway.orchestration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Kafka Event Bus Configuration for Orchestration Service (Microservices Mode).
 * 
 * <p>This configuration is activated only when the 'microservices' profile is active.
 * It enables Kafka as the distributed event bus through the Axon Kafka Spring Boot Starter,
 * allowing Orchestration Service to:
 * <ul>
 *   <li><b>Consume events</b>: From Authorization, Processing, and Settlement services</li>
 *   <li><b>Publish commands</b>: To coordinate the payment processing saga</li>
 * </ul>
 * </p>
 * 
 * <h3>Auto-Configuration:</h3>
 * <p>The Axon Kafka Spring Boot Starter automatically configures:</p>
 * <ul>
 *   <li><b>KafkaPublisher</b>: Publishes commands to Kafka topics</li>
 *   <li><b>KafkaMessageSource</b>: Consumes events from Kafka topics</li>
 *   <li><b>KafkaMessageConverter</b>: Converts between Axon and Kafka message formats</li>
 *   <li><b>ProducerFactory & ConsumerFactory</b>: Create Kafka producers/consumers</li>
 * </ul>
 * 
 * <h3>Saga Orchestration Flow:</h3>
 * <pre>
 * PaymentProcessingSaga (in this service):
 * 
 * 1. START: PaymentAuthorizedEvent (from Authorization Service)
 *    ↓
 * 2. Send: ProcessPaymentCommand → Processing Service
 *    ↓
 * 3. Receive: PaymentProcessedEvent (from Processing Service)
 *    ↓
 * 4. Send: SettlePaymentCommand → Settlement Service
 *    ↓
 * 5. Receive: PaymentSettledEvent (from Settlement Service)
 *    ↓
 * 6. END: Saga completes successfully ✓
 * 
 * COMPENSATION FLOW (on failure):
 * - PaymentProcessingFailedEvent → Send VoidAuthorizationCommand
 * - SettlementFailedEvent → Send VoidAuthorizationCommand
 * - AuthorizationVoidedEvent → End saga (compensated)
 * </pre>
 * 
 * <h3>Events Consumed:</h3>
 * <ul>
 *   <li>PaymentAuthorizedEvent (from Authorization Service)</li>
 *   <li>PaymentAuthorizationDeclinedEvent (from Authorization Service)</li>
 *   <li>PaymentProcessedEvent (from Processing Service)</li>
 *   <li>PaymentProcessingFailedEvent (from Processing Service)</li>
 *   <li>PaymentSettledEvent (from Settlement Service)</li>
 *   <li>SettlementFailedEvent (from Settlement Service)</li>
 *   <li>AuthorizationVoidedEvent (from Authorization Service)</li>
 * </ul>
 * 
 * <h3>Commands Published:</h3>
 * <ul>
 *   <li>ProcessPaymentCommand → Processing Service</li>
 *   <li>SettlePaymentCommand → Settlement Service</li>
 *   <li>VoidAuthorizationCommand → Authorization Service (compensation)</li>
 * </ul>
 * 
 * <h3>Configuration (application.yml):</h3>
 * <pre>
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *     consumer:
 *       group-id: orchestration-service-group
 *       auto-offset-reset: earliest
 *       key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 *       value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
 *     producer:
 *       key-serializer: org.apache.kafka.common.serialization.StringSerializer
 *       value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
 *       acks: all
 *       retries: 3
 * 
 * axon:
 *   kafka:
 *     client-id: orchestration-service
 *     default-topic: payment-events
 *     consumer:
 *       event-processor-mode: tracking
 *     publisher:
 *       confirmation-mode: transactional
 * </pre>
 * 
 * <h3>Deployment Notes:</h3>
 * <ul>
 *   <li><b>Monolith Mode</b>: This config is inactive; uses in-memory event bus</li>
 *   <li><b>Microservices Mode</b>: This config is active; uses Kafka event bus</li>
 *   <li><b>Consumer Group</b>: orchestration-service-group (ensures single saga instance)</li>
 *   <li><b>Event Processing</b>: Tracking mode for saga event handlers</li>
 *   <li><b>Reliability</b>: Transactional publishing with acks=all</li>
 * </ul>
 * 
 * @see com.vsa.paymentgateway.orchestration.saga.PaymentProcessingSaga
 * @see org.axonframework.extensions.kafka.autoconfig.KafkaAutoConfiguration
 * @see org.axonframework.extensions.kafka.eventhandling.KafkaPublisher
 * @author VSA Team
 * @since 1.0.0
 */
@Configuration
@Profile("microservices")
public class KafkaEventBusConfig {
    
    /**
     * This class relies on Axon Kafka Spring Boot Starter auto-configuration.
     * 
     * <p>The axon-kafka-spring-boot-starter dependency automatically creates all
     * necessary beans for Kafka event consumption and command publishing based on
     * the configuration in application.yml (spring.kafka.* and axon.kafka.* properties).</p>
     * 
     * <p>The saga's event handlers will automatically consume events from Kafka when
     * the microservices profile is active, and commands sent by the saga will be
     * published to Kafka for other services to process.</p>
     * 
     * <p>No explicit bean definitions are required unless you need to customize
     * the default behavior (e.g., custom error handling, topic routing, etc.).</p>
     */
}
