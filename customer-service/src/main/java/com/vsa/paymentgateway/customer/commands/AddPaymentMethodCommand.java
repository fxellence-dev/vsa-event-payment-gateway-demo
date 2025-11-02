package com.vsa.paymentgateway.customer.commands;

import com.vsa.paymentgateway.common.commands.PaymentCommand;
import com.vsa.paymentgateway.common.valueobjects.PaymentCard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Command to add a payment method to customer
 * Part of the Customer Onboarding vertical slice
 */
public class AddPaymentMethodCommand extends PaymentCommand {

    @NotNull(message = "Payment card cannot be null")
    @Valid
    private final PaymentCard paymentCard;
    
    private final boolean isDefault;

    public AddPaymentMethodCommand(String customerId, PaymentCard paymentCard, boolean isDefault) {
        super(customerId);
        this.paymentCard = paymentCard;
        this.isDefault = isDefault;
    }

    public String getCustomerId() { return getTargetAggregateIdentifier(); }
    public PaymentCard getPaymentCard() { return paymentCard; }
    public boolean isDefault() { return isDefault; }
}