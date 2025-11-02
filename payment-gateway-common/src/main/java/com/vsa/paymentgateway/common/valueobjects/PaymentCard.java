package com.vsa.paymentgateway.common.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Value Object representing payment card information
 * Immutable and encapsulates card validation logic
 */
public class PaymentCard {
    
    @NotBlank
    @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13-19 digits")
    private final String cardNumber;
    
    @NotBlank
    @Pattern(regexp = "(0[1-9]|1[0-2])", message = "Expiry month must be 01-12")
    private final String expiryMonth;
    
    @NotBlank
    @Pattern(regexp = "\\d{2}", message = "Expiry year must be 2 digits")
    private final String expiryYear;
    
    @NotBlank
    @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3-4 digits")
    private final String cvv;
    
    @NotBlank
    private final String cardHolderName;

    @JsonCreator
    public PaymentCard(@JsonProperty("cardNumber") String cardNumber,
                       @JsonProperty("expiryMonth") String expiryMonth,
                       @JsonProperty("expiryYear") String expiryYear,
                       @JsonProperty("cvv") String cvv,
                       @JsonProperty("cardHolderName") String cardHolderName) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "Card number cannot be null");
        this.expiryMonth = Objects.requireNonNull(expiryMonth, "Expiry month cannot be null");
        this.expiryYear = Objects.requireNonNull(expiryYear, "Expiry year cannot be null");
        this.cvv = Objects.requireNonNull(cvv, "CVV cannot be null");
        this.cardHolderName = Objects.requireNonNull(cardHolderName, "Card holder name cannot be null");
        
        validateCard();
    }

    private void validateCard() {
        if (!isValidLuhn(cardNumber)) {
            throw new IllegalArgumentException("Invalid card number");
        }
    }

    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    public String getCardNumber() { return cardNumber; }
    public String getExpiryMonth() { return expiryMonth; }
    public String getExpiryYear() { return expiryYear; }
    public String getCvv() { return cvv; }
    public String getCardHolderName() { return cardHolderName; }

    public String getMaskedCardNumber() {
        if (cardNumber.length() < 4) return cardNumber;
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    public CardType getCardType() {
        if (cardNumber.startsWith("4")) return CardType.VISA;
        if (cardNumber.startsWith("5") || cardNumber.startsWith("2")) return CardType.MASTERCARD;
        if (cardNumber.startsWith("3")) return CardType.AMEX;
        return CardType.UNKNOWN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentCard that = (PaymentCard) o;
        return Objects.equals(cardNumber, that.cardNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber);
    }

    public enum CardType {
        VISA, MASTERCARD, AMEX, UNKNOWN
    }
}