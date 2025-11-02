package com.vsa.paymentgateway.common.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Objects;

/**
 * Base command for all payment-related domain commands
 * Following Command pattern in VSA with CQRS
 */
public abstract class PaymentCommand {

    @TargetAggregateIdentifier
    @NotBlank(message = "Target aggregate identifier cannot be blank")
    private final String targetAggregateIdentifier;

    protected PaymentCommand(@NotNull String targetAggregateIdentifier) {
        this.targetAggregateIdentifier = Objects.requireNonNull(targetAggregateIdentifier, 
            "Target aggregate identifier cannot be null");
    }

    public String getTargetAggregateIdentifier() {
        return targetAggregateIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentCommand that = (PaymentCommand) o;
        return Objects.equals(targetAggregateIdentifier, that.targetAggregateIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetAggregateIdentifier);
    }
}