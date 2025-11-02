package com.vsa.paymentgateway.authorization.commands;

import com.vsa.paymentgateway.common.commands.PaymentCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Command to void/cancel a previously authorized payment
 * Used for compensation in saga when processing or settlement fails
 */
public class VoidAuthorizationCommand extends PaymentCommand {

    @NotBlank(message = "Reason cannot be blank")
    private final String reason;
    
    private final String compensatingPaymentId;

    public VoidAuthorizationCommand(String authorizationId, String reason, String compensatingPaymentId) {
        super(authorizationId);
        this.reason = reason;
        this.compensatingPaymentId = compensatingPaymentId;
    }

    public String getAuthorizationId() { 
        return getTargetAggregateIdentifier(); 
    }
    
    public String getReason() { 
        return reason; 
    }
    
    public String getCompensatingPaymentId() { 
        return compensatingPaymentId; 
    }
}
