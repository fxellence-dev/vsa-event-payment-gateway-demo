package com.vsa.paymentgateway.common.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Value Object representing a monetary amount with currency
 * Immutable and part of the domain model
 */
public class Money {
    
    @NotBlank
    private final String amount;
    
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid 3-letter ISO code")
    private final String currency;

    @JsonCreator
    public Money(@JsonProperty("amount") String amount, 
                 @JsonProperty("currency") String currency) {
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        
        if (amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be empty");
        }
        
        try {
            double value = Double.parseDouble(amount);
            if (value < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount);
        }
    }

    public String getAmount() { return amount; }
    public String getCurrency() { return currency; }

    public double getAmountAsDouble() {
        return Double.parseDouble(amount);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        double sum = this.getAmountAsDouble() + other.getAmountAsDouble();
        return new Money(String.valueOf(sum), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare amounts with different currencies");
        }
        return this.getAmountAsDouble() > other.getAmountAsDouble();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}