package com.vsa.paymentgateway.settlement.queries;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for Settlement queries
 * Provides optimized view of settlement data
 */
@Entity
@Table(name = "settlement_read_model")
public class SettlementReadModel {
    
    @Id
    private String settlementId;
    
    @Column(nullable = false)
    private String paymentId;
    
    @Column(nullable = false)
    private String processingId;
    
    @Column(nullable = false)
    private String authorizationId;
    
    @Column(nullable = false)
    private String merchantId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;
    
    @Column
    private BigDecimal feeAmount;
    
    @Column
    private BigDecimal netAmount;
    
    @Column
    private String settlementBatchId;
    
    @Column
    private String bankTransactionId;
    
    @Column
    private String failureReason;
    
    @Column
    private String errorCode;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant settledAt;
    
    @Column
    private Instant failedAt;
    
    protected SettlementReadModel() {
    }
    
    public SettlementReadModel(String settlementId, String paymentId, String processingId,
                              String authorizationId, String merchantId, BigDecimal amount, 
                              String currency) {
        this.settlementId = settlementId;
        this.paymentId = paymentId;
        this.processingId = processingId;
        this.authorizationId = authorizationId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.status = SettlementStatus.PENDING;
        this.createdAt = Instant.now();
    }
    
    // Getters
    public String getSettlementId() {
        return settlementId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public String getProcessingId() {
        return processingId;
    }
    
    public String getAuthorizationId() {
        return authorizationId;
    }
    
    public String getMerchantId() {
        return merchantId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public SettlementStatus getStatus() {
        return status;
    }
    
    public BigDecimal getFeeAmount() {
        return feeAmount;
    }
    
    public BigDecimal getNetAmount() {
        return netAmount;
    }
    
    public String getSettlementBatchId() {
        return settlementBatchId;
    }
    
    public String getBankTransactionId() {
        return bankTransactionId;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getSettledAt() {
        return settledAt;
    }
    
    public Instant getFailedAt() {
        return failedAt;
    }
    
    // Setters for projection updates
    public void setStatus(SettlementStatus status) {
        this.status = status;
    }
    
    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }
    
    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }
    
    public void setSettlementBatchId(String settlementBatchId) {
        this.settlementBatchId = settlementBatchId;
    }
    
    public void setBankTransactionId(String bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }
    
    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }
}
