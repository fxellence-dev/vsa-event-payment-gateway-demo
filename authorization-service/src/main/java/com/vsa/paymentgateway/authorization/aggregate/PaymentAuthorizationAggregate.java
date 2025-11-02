package com.vsa.paymentgateway.authorization.aggregate;

import com.vsa.paymentgateway.authorization.commands.AuthorizePaymentCommand;
import com.vsa.paymentgateway.authorization.commands.VoidAuthorizationCommand;
import com.vsa.paymentgateway.authorization.events.PaymentAuthorizedEvent;
import com.vsa.paymentgateway.authorization.events.PaymentAuthorizationDeclinedEvent;
import com.vsa.paymentgateway.authorization.events.AuthorizationVoidedEvent;
import com.vsa.paymentgateway.authorization.service.RiskAssessmentService;
import com.vsa.paymentgateway.authorization.valueobjects.AuthorizationStatus;
import com.vsa.paymentgateway.authorization.valueobjects.RiskAssessment;
import com.vsa.paymentgateway.common.valueobjects.Money;
import com.vsa.paymentgateway.common.valueobjects.PaymentCard;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment Authorization Aggregate - handles authorization logic
 * Part of the Payment Authorization vertical slice
 */
@Aggregate
public class PaymentAuthorizationAggregate {

    @AggregateIdentifier
    private String authorizationId;
    
    private String customerId;
    private Money amount;
    private PaymentCard paymentCard;
    private String merchantId;
    private String description;
    private AuthorizationStatus status;
    private String authorizationCode;
    private String declineReason;
    private Instant authorizedAt;

    // Required for Axon
    protected PaymentAuthorizationAggregate() {}

    @CommandHandler
    public PaymentAuthorizationAggregate(AuthorizePaymentCommand command, RiskAssessmentService riskService) {
        // Business validation
        validateAuthorizationRequest(command);
        
        // Perform risk assessment
        RiskAssessment riskAssessment = riskService.assessRisk(
            command.getCustomerId(),
            command.getAmount(),
            command.getPaymentCard(),
            command.getMerchantId()
        );

        if (riskAssessment.isApproved()) {
            // Generate authorization code
            String authCode = generateAuthorizationCode();
            
            AggregateLifecycle.apply(new PaymentAuthorizedEvent(
                command.getAuthorizationId(),
                command.getCustomerId(),
                command.getAmount(),
                command.getMerchantId(),
                authCode,
                command.getPaymentCard().getMaskedCardNumber(),
                command.getDescription()
            ));
        } else {
            AggregateLifecycle.apply(new PaymentAuthorizationDeclinedEvent(
                command.getAuthorizationId(),
                command.getCustomerId(),
                command.getAmount(),
                command.getMerchantId(),
                riskAssessment.getDeclineReason(),
                riskAssessment.getDeclineCode(),
                command.getPaymentCard().getMaskedCardNumber(),
                command.getDescription()
            ));
        }
    }

    @EventSourcingHandler
    public void on(PaymentAuthorizedEvent event) {
        this.authorizationId = event.getAuthorizationId();
        this.customerId = event.getCustomerId();
        this.amount = event.getAmount();
        this.merchantId = event.getMerchantId();
        this.description = event.getDescription();
        this.status = AuthorizationStatus.AUTHORIZED;
        this.authorizationCode = event.getAuthorizationCode();
        this.authorizedAt = event.getTimestamp();
    }

    @EventSourcingHandler
    public void on(PaymentAuthorizationDeclinedEvent event) {
        this.authorizationId = event.getAuthorizationId();
        this.customerId = event.getCustomerId();
        this.amount = event.getAmount();
        this.merchantId = event.getMerchantId();
        this.description = event.getDescription();
        this.status = AuthorizationStatus.DECLINED;
        this.declineReason = event.getDeclineReason();
    }

    /**
     * Command handler for voiding an authorization
     * Used for compensation when processing or settlement fails
     */
    @CommandHandler
    public void handle(VoidAuthorizationCommand command) {
        // Can only void an authorized payment
        if (this.status != AuthorizationStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Cannot void authorization in status: " + this.status + 
                ". Only AUTHORIZED payments can be voided."
            );
        }
        
        AggregateLifecycle.apply(new AuthorizationVoidedEvent(
            this.authorizationId,
            command.getReason(),
            command.getCompensatingPaymentId(),
            Instant.now()
        ));
    }

    @EventSourcingHandler
    public void on(AuthorizationVoidedEvent event) {
        this.status = AuthorizationStatus.VOIDED;
    }

    private void validateAuthorizationRequest(AuthorizePaymentCommand command) {
        if (command.getAmount().getAmountAsDouble() <= 0) {
            throw new IllegalArgumentException("Authorization amount must be positive");
        }
        
        if (command.getAmount().getAmountAsDouble() > 10000) {
            throw new IllegalArgumentException("Authorization amount exceeds limit");
        }
        
        // Additional validation can be added here
    }

    private String generateAuthorizationCode() {
        return "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters for testing and state verification
    public String getAuthorizationId() { return authorizationId; }
    public AuthorizationStatus getStatus() { return status; }
    public String getAuthorizationCode() { return authorizationCode; }
    public String getDeclineReason() { return declineReason; }
}