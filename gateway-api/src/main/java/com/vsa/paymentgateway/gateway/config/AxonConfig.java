package com.vsa.paymentgateway.gateway.config;

import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.serialization.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Axon Framework configuration to use JPA-based event store
 * All events are persisted to PostgreSQL database
 */
@Configuration
public class AxonConfig {

    /**
     * Configure JPA-based event storage engine
     * Events are stored in PostgreSQL via JPA entities
     */
    @Bean
    @Primary
    public EventStorageEngine eventStorageEngine(
            Serializer serializer,
            EntityManagerProvider entityManagerProvider,
            TransactionManager transactionManager) {
        return JpaEventStorageEngine.builder()
                .snapshotSerializer(serializer)
                .eventSerializer(serializer)
                .entityManagerProvider(entityManagerProvider)
                .transactionManager(transactionManager)
                .build();
    }
}
