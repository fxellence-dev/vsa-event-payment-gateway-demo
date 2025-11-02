package com.vsa.paymentgateway.settlement.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.UUID;

/**
 * Settlement Service
 * Simulates integration with bank/ACH for merchant payouts
 * 
 * In production, this would integrate with:
 * - ACH networks for US merchants
 * - SEPA for European merchants
 * - Other regional payment networks
 */
@Service
public class SettlementService {
    
    private static final Logger logger = LoggerFactory.getLogger(SettlementService.class);
    private final Random random = new Random();
    
    // Simulated success rate (95% success for demo)
    private static final double SUCCESS_RATE = 0.95;
    
    // Standard processing fee (2.9% + $0.30)
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.029"); // 2.9%
    private static final BigDecimal FEE_FIXED = new BigDecimal("0.30"); // $0.30
    
    /**
     * Settle a payment to the merchant
     * 
     * @param paymentId Payment ID
     * @param merchantId Merchant ID  
     * @param amount Amount to settle
     * @param currency Currency code
     * @return SettlementResult indicating success or failure
     */
    public SettlementResult settlePayment(String paymentId, String merchantId, 
                                         BigDecimal amount, String currency) {
        logger.info("Initiating settlement to merchant: merchant={}, payment={}, amount={} {}", 
                   merchantId, paymentId, amount, currency);
        
        try {
            // Calculate fees and net amount
            BigDecimal feeAmount = calculateFee(amount);
            BigDecimal netAmount = amount.subtract(feeAmount);
            
            logger.debug("Settlement breakdown: gross={}, fee={}, net={}", 
                        amount, feeAmount, netAmount);
            
            // Simulate network delay to banking network
            Thread.sleep(150 + random.nextInt(350)); // 150-500ms delay
            
            // Simulate success/failure scenarios
            double outcome = random.nextDouble();
            
            if (outcome < SUCCESS_RATE) {
                // Success scenario
                String settlementBatchId = generateSettlementBatchId();
                String bankTransactionId = generateBankTransactionId();
                
                logger.info("Settlement successful: batch={}, bankTxn={}, merchant={}, net={} {}", 
                           settlementBatchId, bankTransactionId, merchantId, netAmount, currency);
                
                return SettlementResult.success(settlementBatchId, bankTransactionId, 
                                                feeAmount, netAmount);
            } else {
                // Failure scenarios
                String[] failureReasons = {
                    "Invalid merchant bank account",
                    "Bank account closed",
                    "Daily settlement limit exceeded",
                    "Merchant account suspended",
                    "Bank network error"
                };
                
                String[] errorCodes = {
                    "INVALID_ACCOUNT",
                    "ACCOUNT_CLOSED",
                    "LIMIT_EXCEEDED",
                    "ACCOUNT_SUSPENDED",
                    "NETWORK_ERROR"
                };
                
                int failureIndex = random.nextInt(failureReasons.length);
                String reason = failureReasons[failureIndex];
                String code = errorCodes[failureIndex];
                
                logger.warn("Settlement failed: merchant={}, payment={}, reason={}, code={}", 
                           merchantId, paymentId, reason, code);
                
                return SettlementResult.failure(reason, code);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Settlement interrupted: payment={}", paymentId, e);
            return SettlementResult.failure("Settlement interrupted", "SYSTEM_ERROR");
        }
    }
    
    /**
     * Calculate processing fee
     * Formula: (amount * 2.9%) + $0.30
     */
    private BigDecimal calculateFee(BigDecimal amount) {
        BigDecimal percentageFee = amount.multiply(FEE_PERCENTAGE);
        BigDecimal totalFee = percentageFee.add(FEE_FIXED);
        return totalFee.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Generate settlement batch ID
     * In production, this would come from the batch processing system
     */
    private String generateSettlementBatchId() {
        return String.format("BATCH-%d-%s", 
            System.currentTimeMillis(), 
            UUID.randomUUID().toString().substring(0, 6).toUpperCase()
        );
    }
    
    /**
     * Generate bank transaction ID
     * In production, this would come from the bank/ACH network
     */
    private String generateBankTransactionId() {
        return String.format("BANK-%d-%s", 
            System.currentTimeMillis(), 
            UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}
