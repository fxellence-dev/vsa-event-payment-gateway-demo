package com.vsa.paymentgateway.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Authorization Service Microservice Application
 * 
 * This is the main entry point for the Authorization Service when deployed as a standalone microservice.
 * 
 * Business Capabilities:
 * - Payment authorization with fraud detection
 * - Authorization void (compensation for failed payments)
 * - Authorization status queries
 * 
 * Deployment Modes:
 * - Monolith: Included in gateway-api.jar (default)
 * - Microservices: Runs as standalone service on port 8082
 * 
 * Event Communication:
 * - Publishes: PaymentAuthorizedEvent, AuthorizationVoidedEvent
 * - Subscribes: None (but triggers PaymentProcessingSaga)
 * 
 * Integration Points:
 * - Saga Trigger: PaymentAuthorizedEvent starts PaymentProcessingSaga
 * - Compensation: Receives VoidAuthorizationCommand from saga on failure
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
    "com.vsa.paymentgateway.authorization",
    "com.vsa.paymentgateway.gateway.config" // Share common config
})
public class AuthorizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }
}
