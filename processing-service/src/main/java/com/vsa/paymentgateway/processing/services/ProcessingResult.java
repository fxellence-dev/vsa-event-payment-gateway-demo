package com.vsa.paymentgateway.processing.services;

import java.math.BigDecimal;

/**
 * Result of a payment processing attempt
 */
public class ProcessingResult {
    
    private final boolean success;
    private final String transactionId;
    private final String failureReason;
    private final String errorCode;
    
    private ProcessingResult(boolean success, String transactionId, String failureReason, String errorCode) {
        this.success = success;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
        this.errorCode = errorCode;
    }
    
    public static ProcessingResult success(String transactionId) {
        return new ProcessingResult(true, transactionId, null, null);
    }
    
    public static ProcessingResult failure(String failureReason, String errorCode) {
        return new ProcessingResult(false, null, failureReason, errorCode);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
