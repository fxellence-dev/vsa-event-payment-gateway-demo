package com.vsa.paymentgateway.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Processing Service Microservice Application
 * 
 * This is the main entry point for the Processing Service when deployed as a standalone microservice.
 * 
 * Business Capabilities:
 * - Payment processing with external processor integration (Stripe-like)
 * - Payment refunds (compensation for failed settlements)
 * - Processing status queries and transaction tracking
 * 
 * Deployment Modes:
 * - Monolith: Included in gateway-api.jar (default)
 * - Microservices: Runs as standalone service on port 8083
 * 
 * Event Communication:
 * - Publishes: PaymentProcessedEvent, PaymentProcessingFailedEvent, PaymentRefundedEvent
 * - Subscribes: ProcessPaymentCommand (from saga), RefundPaymentCommand (from saga)
 * 
 * Integration Points:
 * - Saga Coordination: Receives ProcessPaymentCommand from PaymentProcessingSaga
 * - Compensation: Receives RefundPaymentCommand when settlement fails
 * - External Systems: Simulates Stripe/Adyen/PayPal API integration
 * 
 * Processing Logic:
 * - 90% success rate (simulated for demo)
 * - Generates transaction IDs from external processor
 * - Handles various failure scenarios (insufficient funds, fraud, expired card)
 * 
 * Technology Stack:
 * - Spring Boot 3.2.0
 * - Axon Framework 4.9.1 (CQRS/Event Sourcing)
 * - Spring Data JPA (Read models)
 * - PostgreSQL (Event store & read models)
 * - Kafka (Event bus in microservices mode)
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.vsa.paymentgateway.processing",
    "com.vsa.paymentgateway.gateway.config" // Share common config
})
public class ProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessingServiceApplication.class, args);
    }
}
