package com.vsa.paymentgateway.settlement.aggregates;

import com.vsa.paymentgateway.settlement.commands.SettlePaymentCommand;
import com.vsa.paymentgateway.settlement.events.PaymentSettledEvent;
import com.vsa.paymentgateway.settlement.events.SettlementFailedEvent;
import com.vsa.paymentgateway.settlement.services.SettlementResult;
import com.vsa.paymentgateway.settlement.services.SettlementService;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aggregate for Payment Settlement
 * Handles settlement of processed payments to merchants
 */
@Aggregate
public class SettlementAggregate {
    
    private static final Logger logger = LoggerFactory.getLogger(SettlementAggregate.class);
    
    @AggregateIdentifier
    private String settlementId;
    
    private String paymentId;
    private String processingId;
    private String authorizationId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private SettlementStatus status;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String settlementBatchId;
    private String bankTransactionId;
    private String failureReason;
    
    // Default constructor required by Axon
    protected SettlementAggregate() {
    }
    
    /**
     * Command handler for settling a payment
     * This simulates interaction with bank/ACH networks
     */
    @CommandHandler
    public SettlementAggregate(SettlePaymentCommand command, SettlementService settlementService) {
        logger.info("Settling payment: {} for merchant: {}, amount: {} {}", 
                   command.getPaymentId(), command.getMerchantId(), 
                   command.getAmount(), command.getCurrency());
        
        // Validate command
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        if (command.getMerchantId() == null || command.getMerchantId().trim().isEmpty()) {
            throw new IllegalArgumentException("Merchant ID is required");
        }
        
        try {
            // Call settlement service (simulates bank/ACH integration)
            SettlementResult result = settlementService.settlePayment(
                command.getPaymentId(),
                command.getMerchantId(),
                command.getAmount(),
                command.getCurrency()
            );
            
            if (result.isSuccess()) {
                // Settlement succeeded
                logger.info("Payment settled successfully: {}, batch: {}, bank txn: {}", 
                           command.getPaymentId(), result.getSettlementBatchId(), 
                           result.getBankTransactionId());
                
                AggregateLifecycle.apply(new PaymentSettledEvent(
                    command.getSettlementId(),
                    command.getPaymentId(),
                    command.getProcessingId(),
                    command.getAuthorizationId(),
                    command.getMerchantId(),
                    command.getAmount(),
                    command.getCurrency(),
                    result.getFeeAmount(),
                    result.getNetAmount(),
                    result.getSettlementBatchId(),
                    Instant.now()
                ));
            } else {
                // Settlement failed
                logger.warn("Payment settlement failed: {}, reason: {}", 
                           command.getPaymentId(), result.getFailureReason());
                
                AggregateLifecycle.apply(new SettlementFailedEvent(
                    command.getSettlementId(),
                    command.getPaymentId(),
                    command.getProcessingId(),
                    command.getAmount(),
                    command.getCurrency(),
                    result.getFailureReason(),
                    result.getErrorCode(),
                    Instant.now()
                ));
            }
        } catch (Exception e) {
            logger.error("Exception during settlement: {}", command.getPaymentId(), e);
            
            AggregateLifecycle.apply(new SettlementFailedEvent(
                command.getSettlementId(),
                command.getPaymentId(),
                command.getProcessingId(),
                command.getAmount(),
                command.getCurrency(),
                "Settlement exception: " + e.getMessage(),
                "SYSTEM_ERROR",
                Instant.now()
            ));
        }
    }
    
    @EventSourcingHandler
    public void on(PaymentSettledEvent event) {
        this.settlementId = event.getSettlementId();
        this.paymentId = event.getPaymentId();
        this.processingId = event.getProcessingId();
        this.authorizationId = event.getAuthorizationId();
        this.merchantId = event.getMerchantId();
        this.amount = event.getAmount();
        this.currency = event.getCurrency();
        this.feeAmount = event.getFeeAmount();
        this.netAmount = event.getNetAmount();
        this.settlementBatchId = event.getSettlementBatchId();
        this.status = SettlementStatus.SETTLED;
        
        logger.debug("Aggregate state updated: Payment settled - {}", this.paymentId);
    }
    
    @EventSourcingHandler
    public void on(SettlementFailedEvent event) {
        this.settlementId = event.getSettlementId();
        this.paymentId = event.getPaymentId();
        this.processingId = event.getProcessingId();
        this.amount = event.getAmount();
        this.currency = event.getCurrency();
        this.failureReason = event.getFailureReason();
        this.status = SettlementStatus.FAILED;
        
        logger.debug("Aggregate state updated: Settlement failed - {}", this.paymentId);
    }
}

enum SettlementStatus {
    PENDING,
    SETTLING,
    SETTLED,
    FAILED
}
