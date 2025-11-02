package com.vsa.paymentgateway.processing.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

/**
 * Command to process a payment after authorization
 */
public class ProcessPaymentCommand {
    
    @TargetAggregateIdentifier
    private final String processingId;
    private final String paymentId;
    private final String authorizationId;
    private final String customerId;
    private final String paymentMethodId;
    private final BigDecimal amount;
    private final String currency;
    
    public ProcessPaymentCommand(String processingId, String paymentId, String authorizationId,
                                String customerId, String paymentMethodId, 
                                BigDecimal amount, String currency) {
        this.processingId = processingId;
        this.paymentId = paymentId;
        this.authorizationId = authorizationId;
        this.customerId = customerId;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.currency = currency;
    }
    
    // Getters
    public String getProcessingId() { return processingId; }
    public String getPaymentId() { return paymentId; }
    public String getAuthorizationId() { return authorizationId; }
    public String getCustomerId() { return customerId; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
