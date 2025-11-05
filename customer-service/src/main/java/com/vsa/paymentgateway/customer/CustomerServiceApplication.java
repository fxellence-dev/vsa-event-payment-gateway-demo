package com.vsa.paymentgateway.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Customer Service Microservice Application
 * 
 * This is the main entry point for the Customer Service when deployed as a standalone microservice.
 * 
 * Business Capabilities:
 * - Customer registration and management
 * - Payment method management
 * - Customer profile queries
 * 
 * Deployment Modes:
 * - Monolith: Included in gateway-api.jar (default)
 * - Microservices: Runs as standalone service on port 8081
 * 
 * Event Communication:
 * - Publishes: CustomerRegisteredEvent, PaymentMethodAddedEvent
 * - Subscribes: None (root aggregate service)
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
    "com.vsa.paymentgateway.customer",
    "com.vsa.paymentgateway.gateway.config" // Share common config
})
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
