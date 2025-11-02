package com.vsa.paymentgateway.processing.queries;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for payment processing
 * Stores the current state of payment processing operations
 */
@Entity
@Table(name = "processing_read_model")
public class ProcessingReadModel {
    
    @Id
    private String processingId;
    
    @Column(nullable = false)
    private String paymentId;
    
    @Column(nullable = false)
    private String authorizationId;
    
    private String customerId;
    private String paymentMethodId;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;
    
    private String processorTransactionId;
    private String failureReason;
    private String errorCode;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    private Instant processedAt;
    private Instant failedAt;
    
    // Default constructor for JPA
    protected ProcessingReadModel() {
    }
    
    public ProcessingReadModel(String processingId, String paymentId, String authorizationId,
                              String customerId, String paymentMethodId, BigDecimal amount, 
                              String currency) {
        this.processingId = processingId;
        this.paymentId = paymentId;
        this.authorizationId = authorizationId;
        this.customerId = customerId;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.currency = currency;
        this.status = ProcessingStatus.PENDING;
        this.createdAt = Instant.now();
    }
    
    // Getters and setters
    public String getProcessingId() { return processingId; }
    public void setProcessingId(String processingId) { this.processingId = processingId; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getAuthorizationId() { return authorizationId; }
    public void setAuthorizationId(String authorizationId) { this.authorizationId = authorizationId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public ProcessingStatus getStatus() { return status; }
    public void setStatus(ProcessingStatus status) { this.status = status; }
    
    public String getProcessorTransactionId() { return processorTransactionId; }
    public void setProcessorTransactionId(String processorTransactionId) { 
        this.processorTransactionId = processorTransactionId; 
    }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
}

enum ProcessingStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
