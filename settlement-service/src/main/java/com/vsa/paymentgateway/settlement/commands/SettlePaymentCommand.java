package com.vsa.paymentgateway.settlement.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

/**
 * Command to settle a payment
 */
public class SettlePaymentCommand {
    
    @TargetAggregateIdentifier
    private final String settlementId;
    private final String paymentId;
    private final String processingId;
    private final String authorizationId;
    private final String merchantId;
    private final BigDecimal amount;
    private final String currency;
    private final BigDecimal feeAmount;
    private final BigDecimal netAmount;
    
    public SettlePaymentCommand(String settlementId, String paymentId, String processingId,
                               String authorizationId, String merchantId, BigDecimal amount, 
                               String currency, BigDecimal feeAmount, BigDecimal netAmount) {
        this.settlementId = settlementId;
        this.paymentId = paymentId;
        this.processingId = processingId;
        this.authorizationId = authorizationId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
    }
    
    // Getters
    public String getSettlementId() { return settlementId; }
    public String getPaymentId() { return paymentId; }
    public String getProcessingId() { return processingId; }
    public String getAuthorizationId() { return authorizationId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
}
