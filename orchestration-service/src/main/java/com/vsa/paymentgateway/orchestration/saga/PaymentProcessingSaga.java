package com.vsa.paymentgateway.orchestration.saga;

import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.spring.stereotype.Saga;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vsa.paymentgateway.orchestration.events.PaymentInitiatedEvent;
import com.vsa.paymentgateway.authorization.events.PaymentAuthorizedEvent;
import com.vsa.paymentgateway.authorization.events.PaymentAuthorizationDeclinedEvent;
import com.vsa.paymentgateway.authorization.events.AuthorizationVoidedEvent;
import com.vsa.paymentgateway.authorization.commands.VoidAuthorizationCommand;
import com.vsa.paymentgateway.processing.events.PaymentProcessedEvent;
import com.vsa.paymentgateway.processing.events.PaymentProcessingFailedEvent;
import com.vsa.paymentgateway.processing.commands.ProcessPaymentCommand;
import com.vsa.paymentgateway.settlement.events.PaymentSettledEvent;
import com.vsa.paymentgateway.settlement.events.SettlementFailedEvent;
import com.vsa.paymentgateway.settlement.commands.SettlePaymentCommand;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Processing Saga - orchestrates the entire payment flow
 * 
 * Complete Flow:
 * 1. PaymentInitiated → Authorize Payment
 * 2. PaymentAuthorized → Process Payment
 * 3. PaymentProcessed → Settle Payment
 * 4. PaymentSettled → END (Success!)
 * 
 * Compensation Flows:
 * - AuthorizationDeclined → END (Failed at authorization)
 * - ProcessingFailed → Void Authorization → END (Compensation)
 * - SettlementFailed → Void Authorization → END (Compensation)
 * 
 * Timeouts:
 * - Payment timeout (5 minutes) → Void Authorization if stuck
 */
@Saga
public class PaymentProcessingSaga {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingSaga.class);
    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(5);
    private static final String PAYMENT_TIMEOUT_DEADLINE = "paymentTimeout";

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    private String paymentId;
    private String customerId;
    private String authorizationId;
    private String processingId;
    private String settlementId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private Instant startedAt;
    private PaymentSagaStatus status;

    @StartSaga
    @EventHandler
    public void handle(PaymentInitiatedEvent event) {
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║  SAGA STARTED: Payment Flow Orchestration                     ║");
        logger.info("╠════════════════════════════════════════════════════════════════╣");
        logger.info("  Payment ID: {}", event.getPaymentId());
        logger.info("  Customer: {}", event.getCustomerId());
        logger.info("  Amount: {}", event.getAmount());
        logger.info("  Merchant: {}", event.getMerchantId());
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        
        this.paymentId = event.getPaymentId();
        this.customerId = event.getCustomerId();
        this.merchantId = event.getMerchantId();
        this.amount = new BigDecimal(event.getAmount().getAmount()); // Parse String to BigDecimal
        this.currency = "USD";
        this.startedAt = event.getTimestamp();
        this.status = PaymentSagaStatus.STARTED;

        // The authorization happens automatically via the gateway-api
        // We just wait for PaymentAuthorizedEvent or PaymentAuthorizationDeclinedEvent
        logger.info("→ Waiting for authorization result...");
    }

    /**
     * Step 2: Payment was authorized successfully
     * Next: Send command to process the payment
     */
    @EventHandler
    public void on(PaymentAuthorizedEvent event) {
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║  STEP 2: Authorization Successful                             ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("  Authorization ID: {}", event.getAuthorizationId());
        logger.info("  Authorization Code: {}", event.getAuthorizationCode());
        
        this.authorizationId = event.getAuthorizationId();
        this.status = PaymentSagaStatus.AUTHORIZED;
        
        // Generate processing ID and send processing command
        this.processingId = UUID.randomUUID().toString();
        
        logger.info("→ Sending ProcessPaymentCommand to processing service...");
        logger.info("  Processing ID: {}", this.processingId);
        
        commandGateway.send(new ProcessPaymentCommand(
            this.processingId,
            this.paymentId,
            this.authorizationId,
            this.customerId,
            null, // paymentMethodId - not available at this stage
            this.amount,
            this.currency
        ));
        
        this.status = PaymentSagaStatus.PROCESSING_PENDING;
    }

    /**
     * Step 3: Payment was processed successfully
     * Next: Send command to settle the payment
     */
    @EventHandler
    public void on(PaymentProcessedEvent event) {
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║  STEP 3: Processing Successful                                ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("  Processing ID: {}", event.getProcessingId());
        logger.info("  Processor Txn ID: {}", event.getProcessorTransactionId());
        logger.info("  Amount: {} {}", event.getAmount(), event.getCurrency());
        
        this.status = PaymentSagaStatus.PROCESSED;
        
        // Generate settlement ID and send settlement command
        this.settlementId = UUID.randomUUID().toString();
        
        logger.info("→ Sending SettlePaymentCommand to settlement service...");
        logger.info("  Settlement ID: {}", this.settlementId);
        
        commandGateway.send(new SettlePaymentCommand(
            this.settlementId,
            this.paymentId,
            this.processingId,
            this.authorizationId,
            this.merchantId,
            event.getAmount(),
            event.getCurrency(),
            null, // feeAmount - will be calculated by settlement service
            null  // netAmount - will be calculated by settlement service
        ));
        
        this.status = PaymentSagaStatus.SETTLEMENT_PENDING;
    }

    /**
     * Step 4: Payment was settled successfully
     * Flow complete! End the saga.
     */
    @EndSaga
    @EventHandler
    public void on(PaymentSettledEvent event) {
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║  STEP 4: Settlement Successful - PAYMENT COMPLETE! ✓          ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("  Settlement ID: {}", event.getSettlementId());
        logger.info("  Batch ID: {}", event.getSettlementBatchId());
        logger.info("  Gross Amount: {} {}", event.getAmount(), event.getCurrency());
        logger.info("  Fee: {} {}", event.getFeeAmount(), event.getCurrency());
        logger.info("  Net Amount: {} {}", event.getNetAmount(), event.getCurrency());
        logger.info("  Merchant: {}", event.getMerchantId());
        logger.info("");
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║  ✓ SAGA COMPLETED SUCCESSFULLY                                ║");
        logger.info("║  Payment ID: {}", String.format("%-48s", this.paymentId) + "║");
        logger.info("║  Total Time: {} ms", String.format("%-47s", 
            java.time.Duration.between(this.startedAt, Instant.now()).toMillis()) + "║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        
        this.status = PaymentSagaStatus.COMPLETED;
    }

    // ======================== COMPENSATION FLOWS ========================

    /**
     * Compensation: Authorization was declined
     * End saga immediately (nothing to compensate)
     */
    @EndSaga
    @EventHandler
    public void on(PaymentAuthorizationDeclinedEvent event) {
        logger.warn("╔════════════════════════════════════════════════════════════════╗");
        logger.warn("║  ✗ AUTHORIZATION DECLINED                                     ║");
        logger.warn("╚════════════════════════════════════════════════════════════════╝");
        logger.warn("  Reason: {}", event.getDeclineReason());
        logger.warn("  Code: {}", event.getDeclineCode());
        logger.warn("");
        logger.warn("  → Saga ending (no compensation needed)");
        
        this.status = PaymentSagaStatus.FAILED;
    }

    /**
     * Compensation: Processing failed
     * Need to void the authorization
     */
    @EventHandler
    public void on(PaymentProcessingFailedEvent event) {
        logger.warn("╔════════════════════════════════════════════════════════════════╗");
        logger.warn("║  ✗ PROCESSING FAILED - Starting Compensation                  ║");
        logger.warn("╚════════════════════════════════════════════════════════════════╝");
        logger.warn("  Reason: {}", event.getFailureReason());
        logger.warn("  Error Code: {}", event.getProcessorErrorCode());
        logger.warn("");
        logger.warn("  → COMPENSATION: Voiding authorization {}", this.authorizationId);
        
        this.status = PaymentSagaStatus.PROCESSING_FAILED;
        
        // Send void authorization command
        commandGateway.send(new VoidAuthorizationCommand(
            this.authorizationId,
            "Processing failed: " + event.getFailureReason(),
            this.paymentId
        ));
        
        this.status = PaymentSagaStatus.AUTHORIZATION_VOIDING;
    }

    /**
     * Compensation: Settlement failed
     * Need to void the authorization (processing already completed)
     */
    @EventHandler
    public void on(SettlementFailedEvent event) {
        logger.warn("╔════════════════════════════════════════════════════════════════╗");
        logger.warn("║  ✗ SETTLEMENT FAILED - Starting Compensation                  ║");
        logger.warn("╚════════════════════════════════════════════════════════════════╝");
        logger.warn("  Reason: {}", event.getFailureReason());
        logger.warn("  Error Code: {}", event.getErrorCode());
        logger.warn("");
        logger.warn("  → COMPENSATION: Voiding authorization {}", this.authorizationId);
        logger.warn("  Note: In production, would also refund the processed payment");
        
        this.status = PaymentSagaStatus.SETTLEMENT_FAILED;
        
        // Send void authorization command
        commandGateway.send(new VoidAuthorizationCommand(
            this.authorizationId,
            "Settlement failed: " + event.getFailureReason(),
            this.paymentId
        ));
        
        this.status = PaymentSagaStatus.AUTHORIZATION_VOIDING;
    }

    /**
     * Compensation complete: Authorization voided
     * End saga after compensation
     */
    @EndSaga
    @EventHandler
    public void on(AuthorizationVoidedEvent event) {
        logger.warn("╔════════════════════════════════════════════════════════════════╗");
        logger.warn("║  ✓ COMPENSATION COMPLETE: Authorization Voided                ║");
        logger.warn("╚════════════════════════════════════════════════════════════════╝");
        logger.warn("  Authorization ID: {}", event.getAuthorizationId());
        logger.warn("  Reason: {}", event.getReason());
        logger.warn("");
        logger.warn("╔════════════════════════════════════════════════════════════════╗");
        logger.warn("║  ✗ SAGA FAILED (Compensation Completed)                       ║");
        logger.warn("║  Payment ID: {}", String.format("%-48s", this.paymentId) + "║");
        logger.warn("╚════════════════════════════════════════════════════════════════╝");
        
        this.status = PaymentSagaStatus.FAILED;
    }

    // ======================== TIMEOUT HANDLING ========================

    /**
     * Timeout handler - payment took too long
     * Void authorization if still in progress
     */
    @DeadlineHandler(deadlineName = PAYMENT_TIMEOUT_DEADLINE)
    public void onPaymentTimeout() {
        logger.error("╔════════════════════════════════════════════════════════════════╗");
        logger.error("║  ✗ PAYMENT TIMEOUT                                            ║");
        logger.error("╚════════════════════════════════════════════════════════════════╝");
        logger.error("  Payment ID: {}", this.paymentId);
        logger.error("  Status: {}", this.status);
        logger.error("");
        
        if (this.authorizationId != null && 
            (this.status == PaymentSagaStatus.PROCESSING_PENDING || 
             this.status == PaymentSagaStatus.SETTLEMENT_PENDING)) {
            
            logger.error("  → COMPENSATION: Voiding authorization due to timeout");
            
            commandGateway.send(new VoidAuthorizationCommand(
                this.authorizationId,
                "Payment timeout after " + PAYMENT_TIMEOUT.toMinutes() + " minutes",
                this.paymentId
            ));
            
            this.status = PaymentSagaStatus.TIMEOUT;
        }
    }
}

enum PaymentSagaStatus {
    STARTED,                  // Saga initiated
    AUTHORIZATION_PENDING,    // Waiting for authorization
    AUTHORIZED,               // Authorization successful
    PROCESSING_PENDING,       // Processing command sent
    PROCESSED,                // Processing successful
    SETTLEMENT_PENDING,       // Settlement command sent
    COMPLETED,                // All steps successful!
    FAILED,                   // Authorization declined or compensation completed
    TIMEOUT,                  // Payment timeout
    PROCESSING_FAILED,        // Processing failed (before compensation)
    SETTLEMENT_FAILED,        // Settlement failed (before compensation)
    AUTHORIZATION_VOIDING,    // Compensation: voiding authorization
    REFUNDING,                // Compensation: refunding payment (future)
    REFUNDED                  // Compensation complete (future)
}