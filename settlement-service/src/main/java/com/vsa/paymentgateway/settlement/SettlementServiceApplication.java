package com.vsa.paymentgateway.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Settlement Service Microservice Application
 * 
 * This is the main entry point for the Settlement Service when deployed as a standalone microservice.
 * 
 * Business Capabilities:
 * - Payment settlement with merchant payouts
 * - Batch settlement processing (T+1 settlement)
 * - Settlement status queries and reconciliation
 * 
 * Deployment Modes:
 * - Monolith: Included in gateway-api.jar (default)
 * - Microservices: Runs as standalone service on port 8084
 * 
 * Event Communication:
 * - Publishes: PaymentSettledEvent, SettlementFailedEvent
 * - Subscribes: SettlePaymentCommand (from saga)
 * 
 * Integration Points:
 * - Saga Coordination: Receives SettlePaymentCommand from PaymentProcessingSaga
 * - External Systems: Simulates Bank/ACH integration for merchant payouts
 * - Batch Processing: Groups settlements by merchant and date
 * 
 * Settlement Logic:
 * - 95% success rate (simulated for demo)
 * - Calculates processing fees (2.9% + $0.30)
 * - T+1 settlement schedule (settlement date is next business day)
 * - Generates batch IDs and bank transaction IDs
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
    "com.vsa.paymentgateway.settlement",
    "com.vsa.paymentgateway.gateway.config" // Share common config
})
public class SettlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementServiceApplication.class, args);
    }
}
