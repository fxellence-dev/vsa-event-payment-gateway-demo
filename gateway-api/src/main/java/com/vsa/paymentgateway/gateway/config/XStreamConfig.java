package com.vsa.paymentgateway.gateway.config;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XStream configuration for Axon serialization
 * Allows all domain event classes to be serialized/deserialized
 */
@Configuration
public class XStreamConfig {

    @Bean
    public XStream xStream() {
        XStream xStream = new XStream();
        
        // Allow all types from our application packages
        xStream.allowTypesByWildcard(new String[]{
            "com.vsa.paymentgateway.**"
        });
        
        // For development/demo purposes, allow all types
        // In production, be more specific about allowed types
        xStream.addPermission(AnyTypePermission.ANY);
        
        return xStream;
    }
}
