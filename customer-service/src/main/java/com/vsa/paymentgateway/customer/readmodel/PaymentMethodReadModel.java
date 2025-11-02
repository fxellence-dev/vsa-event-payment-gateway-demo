package com.vsa.paymentgateway.customer.readmodel;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Payment method read model for queries - part of CQRS read side
 * Part of the Customer Onboarding vertical slice
 */
@Entity
@Table(name = "payment_method_read_model")
public class PaymentMethodReadModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private CustomerReadModel customer;
    
    @Column(nullable = false)
    private String maskedCardNumber;
    
    @Column(nullable = false)
    private String cardType;
    
    @Column(nullable = false)
    private String expiryMonth;
    
    @Column(nullable = false)
    private String expiryYear;
    
    @Column(nullable = false)
    private String cardHolderName;
    
    @Column(nullable = false)
    private boolean isDefault;
    
    @Column(nullable = false)
    private Instant addedAt;

    // Default constructor for JPA
    protected PaymentMethodReadModel() {}

    public PaymentMethodReadModel(CustomerReadModel customer, String maskedCardNumber, 
                                  String cardType, String expiryMonth, String expiryYear,
                                  String cardHolderName, boolean isDefault, Instant addedAt) {
        this.customer = customer;
        this.maskedCardNumber = maskedCardNumber;
        this.cardType = cardType;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cardHolderName = cardHolderName;
        this.isDefault = isDefault;
        this.addedAt = addedAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerReadModel getCustomer() { return customer; }
    public void setCustomer(CustomerReadModel customer) { this.customer = customer; }

    public String getMaskedCardNumber() { return maskedCardNumber; }
    public void setMaskedCardNumber(String maskedCardNumber) { this.maskedCardNumber = maskedCardNumber; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }

    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}