package com.vsa.paymentgateway.authorization.api;

import com.vsa.paymentgateway.authorization.commands.AuthorizePaymentCommand;
import com.vsa.paymentgateway.authorization.commands.VoidAuthorizationCommand;
import com.vsa.paymentgateway.common.valueobjects.Money;
import com.vsa.paymentgateway.common.valueobjects.PaymentCard;
import jakarta.validation.Valid;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST Controller for Payment Authorization operations
 * Handles authorization requests and queries
 */
@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    private final CommandGateway commandGateway;

    public AuthorizationController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    /**
     * Authorize a payment
     * This triggers the PaymentProcessingSaga!
     */
    @PostMapping("/authorize")
    public ResponseEntity<AuthorizePaymentResponse> authorizePayment(
            @Valid @RequestBody AuthorizePaymentRequest request) {
        
        // Create Money object from amount and currency
        Money money = new Money(request.getAmount().toString(), request.getCurrency());
        
        // Create PaymentCard object - simplified for demo
        // In a real system, paymentMethodId would reference an encrypted card token
        String maskedCard = request.getPaymentMethodId().length() >= 4 
            ? "****" + request.getPaymentMethodId().substring(request.getPaymentMethodId().length() - 4)
            : "****" + request.getPaymentMethodId();
            
        PaymentCard paymentCard = new PaymentCard(
            maskedCard,
            "12",
            "2025",
            "***",
            "Cardholder"
        );
        
        AuthorizePaymentCommand command = new AuthorizePaymentCommand(
            request.getAuthorizationId(),
            request.getCustomerId(),
            money,
            paymentCard,
            request.getMerchantId(),
            "Payment authorization for payment ID: " + request.getPaymentId()
        );

        try {
            commandGateway.sendAndWait(command);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthorizePaymentResponse(
                            request.getAuthorizationId(),
                            "AUTHORIZED",
                            "Payment authorization initiated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new AuthorizePaymentResponse(
                            request.getAuthorizationId(),
                            "FAILED",
                            "Failed to authorize payment: " + e.getMessage()));
        }
    }

    /**
     * Void an authorization
     */
    @PostMapping("/{authorizationId}/void")
    public ResponseEntity<String> voidAuthorization(
            @PathVariable String authorizationId,
            @RequestBody(required = false) VoidAuthorizationRequest request) {
        
        String reason = request != null ? request.getReason() : "Manual void";
        
        VoidAuthorizationCommand command = new VoidAuthorizationCommand(
            authorizationId,
            reason,
            null  // compensatingPaymentId
        );

        try {
            commandGateway.sendAndWait(command);
            return ResponseEntity.ok("Authorization voided successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to void authorization: " + e.getMessage());
        }
    }

    /**
     * Get authorization details
     * Note: Query side not yet implemented - will be added with read model
     */
    @GetMapping("/{authorizationId}")
    public ResponseEntity<String> getAuthorization(
            @PathVariable String authorizationId) {
        
        return ResponseEntity.ok("Authorization query not yet implemented. Check saga logs for authorization: " + authorizationId);
    }

    /**
     * Get authorizations by payment ID  
     * Note: Query side not yet implemented - will be added with read model
     */
    @GetMapping("/by-payment/{paymentId}")
    public ResponseEntity<String> getAuthorizationByPaymentId(
            @PathVariable String paymentId) {
        
        return ResponseEntity.ok("Authorization query not yet implemented. Check saga logs for payment: " + paymentId);
    }

    /**
     * Get authorizations by customer ID
     * Note: Query side not yet implemented - will be added with read model
     */
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<String> getAuthorizationsByCustomerId(
            @PathVariable String customerId) {
        
        return ResponseEntity.ok("Authorization query not yet implemented. Check saga logs for customer: " + customerId);
    }

    // Request/Response DTOs

    public static class AuthorizePaymentRequest {
        private String authorizationId;
        private String paymentId;
        private String customerId;
        private String merchantId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethodId;

        // Getters and Setters
        public String getAuthorizationId() { return authorizationId; }
        public void setAuthorizationId(String authorizationId) { this.authorizationId = authorizationId; }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getPaymentMethodId() { return paymentMethodId; }
        public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    }

    public static class AuthorizePaymentResponse {
        private String authorizationId;
        private String status;
        private String message;

        public AuthorizePaymentResponse(String authorizationId, String status, String message) {
            this.authorizationId = authorizationId;
            this.status = status;
            this.message = message;
        }

        // Getters
        public String getAuthorizationId() { return authorizationId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class VoidAuthorizationRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
