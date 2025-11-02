package com.vsa.paymentgateway.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for Payment Gateway VSA Demo
 * This is the entry point that brings together all vertical slices
 * Uses JPA-based event store for Axon Framework
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.vsa.paymentgateway.common",
    "com.vsa.paymentgateway.customer",
    "com.vsa.paymentgateway.authorization",
    "com.vsa.paymentgateway.processing",
    "com.vsa.paymentgateway.settlement",
    "com.vsa.paymentgateway.orchestration",
    "com.vsa.paymentgateway.gateway"
})
@EntityScan(basePackages = {
    "com.vsa.paymentgateway.customer.readmodel",
    "com.vsa.paymentgateway.authorization.readmodel",
    "com.vsa.paymentgateway.processing.queries",  // Processing read models
    "com.vsa.paymentgateway.settlement.queries",   // Settlement read models
    "org.axonframework.eventhandling.tokenstore.jpa",
    "org.axonframework.modelling.saga.repository.jpa",
    "org.axonframework.eventhandling.deadletter.jpa",
    "org.axonframework.eventsourcing.eventstore.jpa"  // Add event store entities
})
@EnableJpaRepositories(basePackages = {
    "com.vsa.paymentgateway.customer.repository",
    "com.vsa.paymentgateway.authorization.repository",
    "com.vsa.paymentgateway.processing.queries",   // Processing repositories
    "com.vsa.paymentgateway.settlement.queries"     // Settlement repositories
})
public class PaymentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}