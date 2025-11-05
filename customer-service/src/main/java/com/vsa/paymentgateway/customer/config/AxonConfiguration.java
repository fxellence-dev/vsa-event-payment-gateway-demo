package com.vsa.paymentgateway.customer.config;

import com.thoughtworks.xstream.XStream;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Axon Framework configuration for Customer Service
 * Configures event processing for read model projections
 */
@Configuration
@Profile("microservices")
public class AxonConfiguration {

    /**
     * Configure XStream for safe deserialization
     */
    @Bean
    public XStream xStream() {
        XStream xStream = new XStream();
        
        // Allow all classes from our packages
        xStream.allowTypesByWildcard(new String[]{
            "com.vsa.paymentgateway.**"
        });
        
        return xStream;
    }

    /**
     * Configure event processing for Customer projections
     * This configures projections to use the event store (not Kafka consumption)
     * for building read models
     */
    @Autowired
    public void configureEventProcessing(EventProcessingConfigurer configurer) {
        
        // Configure tracking event processor for customer projections
        // This will replay events from the event store to build read models
        configurer.registerTrackingEventProcessor(
            "CustomerProjection",  // Processor name
            org.axonframework.config.Configuration::eventStore,
            c -> TrackingEventProcessorConfiguration.forSingleThreadedProcessing()
        );
        
        // Assign the projection to the processor
        configurer.assignHandlerInstancesMatching(
            "CustomerProjection",
            eventHandler -> eventHandler.getClass().getName().contains("CustomerProjection")
        );
    }
}