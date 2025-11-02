package com.vsa.paymentgateway.customer.readmodel;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer read model for queries - part of CQRS read side
 * Part of the Customer Onboarding vertical slice
 */
@Entity
@Table(name = "customer_read_model")
public class CustomerReadModel {

    @Id
    private String customerId;
    
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    private String phoneNumber;
    private String address;
    
    @Column(nullable = false)
    private Instant registeredAt;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<PaymentMethodReadModel> paymentMethods = new ArrayList<>();

    // Default constructor for JPA
    protected CustomerReadModel() {}

    public CustomerReadModel(String customerId, String customerName, String email, 
                             String phoneNumber, String address, Instant registeredAt) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.registeredAt = registeredAt;
    }

    // Getters and setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }

    public List<PaymentMethodReadModel> getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(List<PaymentMethodReadModel> paymentMethods) { this.paymentMethods = paymentMethods; }
}