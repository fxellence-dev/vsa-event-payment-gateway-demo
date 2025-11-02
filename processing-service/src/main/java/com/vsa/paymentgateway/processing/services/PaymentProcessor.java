package com.vsa.paymentgateway.processing.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Payment Processor Service
 * Simulates integration with external payment processors (Stripe, Adyen, PayPal, etc.)
 * 
 * In production, this would make HTTP calls to actual payment processor APIs
 */
@Service
public class PaymentProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessor.class);
    private final Random random = new Random();
    
    // Simulated success rate (90% success for demo)
    private static final double SUCCESS_RATE = 0.9;
    
    /**
     * Process a payment through the payment processor
     * 
     * @param paymentId Internal payment ID
     * @param authorizationId Authorization ID from authorization service
     * @param amount Amount to process
     * @param currency Currency code (USD, EUR, etc.)
     * @param paymentMethodId Payment method ID (card token, account ID, etc.)
     * @return ProcessingResult indicating success or failure
     */
    public ProcessingResult processPayment(String paymentId, String authorizationId, 
                                          BigDecimal amount, String currency, 
                                          String paymentMethodId) {
        logger.info("Sending payment to processor: payment={}, auth={}, amount={} {}", 
                   paymentId, authorizationId, amount, currency);
        
        try {
            // Simulate network delay to payment processor
            Thread.sleep(100 + random.nextInt(300)); // 100-400ms delay
            
            // Simulate success/failure scenarios
            double outcome = random.nextDouble();
            
            if (outcome < SUCCESS_RATE) {
                // Success scenario
                String transactionId = generateProcessorTransactionId();
                logger.info("Processor approved payment: txnId={}, payment={}", transactionId, paymentId);
                return ProcessingResult.success(transactionId);
            } else {
                // Failure scenarios
                String[] failureReasons = {
                    "Insufficient funds",
                    "Card declined by issuer",
                    "Suspected fraud",
                    "Card expired",
                    "Invalid card number"
                };
                
                String[] errorCodes = {
                    "INSUFFICIENT_FUNDS",
                    "CARD_DECLINED",
                    "FRAUD_SUSPECTED",
                    "CARD_EXPIRED",
                    "INVALID_CARD"
                };
                
                int failureIndex = random.nextInt(failureReasons.length);
                String reason = failureReasons[failureIndex];
                String code = errorCodes[failureIndex];
                
                logger.warn("Processor declined payment: payment={}, reason={}, code={}", 
                           paymentId, reason, code);
                return ProcessingResult.failure(reason, code);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Payment processing interrupted: {}", paymentId, e);
            return ProcessingResult.failure("Processing interrupted", "SYSTEM_ERROR");
        }
    }
    
    /**
     * Generate a simulated processor transaction ID
     * In production, this would come from the actual payment processor
     */
    private String generateProcessorTransactionId() {
        // Format: TXN-{timestamp}-{random}
        return String.format("TXN-%d-%s", 
            System.currentTimeMillis(), 
            UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}
