package com.vsa.paymentgateway.authorization.commands;

import com.vsa.paymentgateway.common.commands.PaymentCommand;
import com.vsa.paymentgateway.common.valueobjects.Money;
import com.vsa.paymentgateway.common.valueobjects.PaymentCard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command to authorize a payment
 * Part of the Payment Authorization vertical slice
 */
public class AuthorizePaymentCommand extends PaymentCommand {

    @NotBlank(message = "Customer ID cannot be blank")
    private final String customerId;
    
    @NotNull(message = "Amount cannot be null")
    @Valid
    private final Money amount;
    
    @NotNull(message = "Payment card cannot be null")
    @Valid
    private final PaymentCard paymentCard;
    
    @NotBlank(message = "Merchant ID cannot be blank")
    private final String merchantId;
    
    private final String description;

    public AuthorizePaymentCommand(String authorizationId, String customerId, Money amount, 
                                   PaymentCard paymentCard, String merchantId, String description) {
        super(authorizationId);
        this.customerId = customerId;
        this.amount = amount;
        this.paymentCard = paymentCard;
        this.merchantId = merchantId;
        this.description = description;
    }

    public String getAuthorizationId() { return getTargetAggregateIdentifier(); }
    public String getCustomerId() { return customerId; }
    public Money getAmount() { return amount; }
    public PaymentCard getPaymentCard() { return paymentCard; }
    public String getMerchantId() { return merchantId; }
    public String getDescription() { return description; }
}