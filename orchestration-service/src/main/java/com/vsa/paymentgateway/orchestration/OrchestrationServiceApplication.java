package com.vsa.paymentgateway.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Orchestration Service Microservice Application
 * 
 * This is the main entry point for the Orchestration Service when deployed as a standalone microservice.
 * 
 * Business Capabilities:
 * - Saga-based workflow orchestration
 * - Multi-service transaction coordination
 * - Automatic compensation on failures
 * - Timeout management for long-running processes
 * 
 * Deployment Modes:
 * - Monolith: Included in gateway-api.jar (default)
 * - Microservices: Runs as standalone service on port 8085
 * 
 * Event Communication:
 * - Publishes: ProcessPaymentCommand, SettlePaymentCommand, VoidAuthorizationCommand, RefundPaymentCommand
 * - Subscribes: PaymentAuthorizedEvent, PaymentProcessedEvent, PaymentProcessingFailedEvent,
 *              PaymentSettledEvent, SettlementFailedEvent, AuthorizationVoidedEvent, PaymentRefundedEvent
 * 
 * Saga Coordination:
 * - PaymentProcessingSaga: Orchestrates Authorization → Processing → Settlement
 * - Success Flow: Authorization → Processing → Settlement → Complete
 * - Failure Flow 1: Processing fails → Void Authorization
 * - Failure Flow 2: Settlement fails → Refund Payment
 * 
 * State Management:
 * - Saga instances stored in PostgreSQL (saga_entry table)
 * - Associations tracked by authorizationId
 * - Timeout: 5-minute deadline for saga completion
 * 
 * Technology Stack:
 * - Spring Boot 3.2.0
 * - Axon Framework 4.9.1 (CQRS/Event Sourcing/Saga)
 * - PostgreSQL (Saga state storage)
 * - Kafka (Event bus in microservices mode)
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.vsa.paymentgateway.orchestration",
    "com.vsa.paymentgateway.gateway.config" // Share common config
})
public class OrchestrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestrationServiceApplication.class, args);
    }
}
