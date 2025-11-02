package com.vsa.paymentgateway.processing.aggregates;

import com.vsa.paymentgateway.processing.commands.ProcessPaymentCommand;
import com.vsa.paymentgateway.processing.events.PaymentProcessedEvent;
import com.vsa.paymentgateway.processing.events.PaymentProcessingFailedEvent;
import com.vsa.paymentgateway.processing.services.PaymentProcessor;
import com.vsa.paymentgateway.processing.services.ProcessingResult;
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
 * Aggregate for Payment Processing
 * Handles the actual payment processing logic
 */
@Aggregate
public class PaymentProcessingAggregate {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingAggregate.class);
    
    @AggregateIdentifier
    private String processingId;
    
    private String paymentId;
    private String authorizationId;
    private String customerId;
    private String paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private ProcessingStatus status;
    private String processorTransactionId;
    private String failureReason;
    
    // Default constructor required by Axon
    protected PaymentProcessingAggregate() {
    }
    
    /**
     * Command handler for processing a payment
     * This simulates interaction with an external payment processor (Stripe, Adyen, etc.)
     */
    @CommandHandler
    public PaymentProcessingAggregate(ProcessPaymentCommand command, PaymentProcessor processor) {
        logger.info("Processing payment: {} for amount: {} {}", 
                   command.getPaymentId(), command.getAmount(), command.getCurrency());
        
        // Validate command
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        if (command.getCurrency() == null || command.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }
        
        try {
            // Call payment processor (simulated external service)
            ProcessingResult result = processor.processPayment(
                command.getPaymentId(),
                command.getAuthorizationId(),
                command.getAmount(),
                command.getCurrency(),
                command.getPaymentMethodId()
            );
            
            if (result.isSuccess()) {
                // Processing succeeded
                logger.info("Payment processed successfully: {}, transaction ID: {}", 
                           command.getPaymentId(), result.getTransactionId());
                
                AggregateLifecycle.apply(new PaymentProcessedEvent(
                    command.getProcessingId(),
                    command.getPaymentId(),
                    command.getAuthorizationId(),
                    result.getTransactionId(),
                    command.getAmount(),
                    command.getCurrency(),
                    Instant.now()
                ));
            } else {
                // Processing failed
                logger.warn("Payment processing failed: {}, reason: {}", 
                           command.getPaymentId(), result.getFailureReason());
                
                AggregateLifecycle.apply(new PaymentProcessingFailedEvent(
                    command.getProcessingId(),
                    command.getPaymentId(),
                    command.getAuthorizationId(),
                    command.getAmount(),
                    command.getCurrency(),
                    result.getFailureReason(),
                    result.getErrorCode(),
                    Instant.now()
                ));
            }
        } catch (Exception e) {
            logger.error("Exception during payment processing: {}", command.getPaymentId(), e);
            
            AggregateLifecycle.apply(new PaymentProcessingFailedEvent(
                command.getProcessingId(),
                command.getPaymentId(),
                command.getAuthorizationId(),
                command.getAmount(),
                command.getCurrency(),
                "Processing exception: " + e.getMessage(),
                "SYSTEM_ERROR",
                Instant.now()
            ));
        }
    }
    
    @EventSourcingHandler
    public void on(PaymentProcessedEvent event) {
        this.processingId = event.getProcessingId();
        this.paymentId = event.getPaymentId();
        this.authorizationId = event.getAuthorizationId();
        this.amount = event.getAmount();
        this.currency = event.getCurrency();
        this.processorTransactionId = event.getProcessorTransactionId();
        this.status = ProcessingStatus.PROCESSED;
        
        logger.debug("Aggregate state updated: Payment processed - {}", this.paymentId);
    }
    
    @EventSourcingHandler
    public void on(PaymentProcessingFailedEvent event) {
        this.processingId = event.getProcessingId();
        this.paymentId = event.getPaymentId();
        this.authorizationId = event.getAuthorizationId();
        this.amount = event.getAmount();
        this.currency = event.getCurrency();
        this.failureReason = event.getFailureReason();
        this.status = ProcessingStatus.FAILED;
        
        logger.debug("Aggregate state updated: Payment processing failed - {}", this.paymentId);
    }
}

enum ProcessingStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
