package com.vsa.paymentgateway.settlement.services;

import java.math.BigDecimal;

/**
 * Result of a settlement attempt
 */
public class SettlementResult {
    
    private final boolean success;
    private final String settlementBatchId;
    private final String bankTransactionId;
    private final BigDecimal feeAmount;
    private final BigDecimal netAmount;
    private final String failureReason;
    private final String errorCode;
    
    private SettlementResult(boolean success, String settlementBatchId, String bankTransactionId,
                            BigDecimal feeAmount, BigDecimal netAmount, 
                            String failureReason, String errorCode) {
        this.success = success;
        this.settlementBatchId = settlementBatchId;
        this.bankTransactionId = bankTransactionId;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.failureReason = failureReason;
        this.errorCode = errorCode;
    }
    
    public static SettlementResult success(String settlementBatchId, String bankTransactionId,
                                          BigDecimal feeAmount, BigDecimal netAmount) {
        return new SettlementResult(true, settlementBatchId, bankTransactionId, 
                                    feeAmount, netAmount, null, null);
    }
    
    public static SettlementResult failure(String failureReason, String errorCode) {
        return new SettlementResult(false, null, null, null, null, failureReason, errorCode);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getSettlementBatchId() {
        return settlementBatchId;
    }
    
    public String getBankTransactionId() {
        return bankTransactionId;
    }
    
    public BigDecimal getFeeAmount() {
        return feeAmount;
    }
    
    public BigDecimal getNetAmount() {
        return netAmount;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
