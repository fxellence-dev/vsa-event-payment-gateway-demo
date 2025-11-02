package com.vsa.paymentgateway.customer.aggregate;

import com.vsa.paymentgateway.common.valueobjects.PaymentCard;
import com.vsa.paymentgateway.customer.commands.AddPaymentMethodCommand;
import com.vsa.paymentgateway.customer.commands.RegisterCustomerCommand;
import com.vsa.paymentgateway.customer.events.CustomerRegisteredEvent;
import com.vsa.paymentgateway.customer.events.PaymentMethodAddedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer Aggregate - represents the write model in CQRS
 * Part of the Customer Onboarding vertical slice
 * 
 * This aggregate handles all customer-related commands and maintains
 * the business logic and invariants for customer operations
 */
@Aggregate
public class CustomerAggregate {

    @AggregateIdentifier
    private String customerId;
    
    private String customerName;
    private String email;
    private String phoneNumber;
    private String address;
    private List<PaymentCard> paymentMethods;
    private PaymentCard defaultPaymentMethod;

    // Required for Axon
    protected CustomerAggregate() {
        this.paymentMethods = new ArrayList<>();
    }

    @CommandHandler
    public CustomerAggregate(RegisterCustomerCommand command) {
        // Business validation
        if (command.getCustomerName() == null || command.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        
        if (command.getEmail() == null || !isValidEmail(command.getEmail())) {
            throw new IllegalArgumentException("Valid email is required");
        }

        // Apply event - this will call the @EventSourcingHandler
        AggregateLifecycle.apply(new CustomerRegisteredEvent(
            command.getCustomerId(),
            command.getCustomerName(),
            command.getEmail(),
            command.getPhoneNumber(),
            command.getAddress()
        ));
    }

    @CommandHandler
    public void handle(AddPaymentMethodCommand command) {
        // Business validation
        if (this.customerId == null) {
            throw new IllegalStateException("Customer must be registered before adding payment methods");
        }
        
        if (command.getPaymentCard() == null) {
            throw new IllegalArgumentException("Payment card cannot be null");
        }
        
        // Check if payment method already exists
        if (paymentMethods.stream().anyMatch(card -> 
            card.getCardNumber().equals(command.getPaymentCard().getCardNumber()))) {
            throw new IllegalArgumentException("Payment method already exists");
        }

        // Apply event
        AggregateLifecycle.apply(new PaymentMethodAddedEvent(
            this.customerId,
            command.getPaymentCard(),
            command.isDefault() || paymentMethods.isEmpty() // First card is always default
        ));
    }

    @EventSourcingHandler
    public void on(CustomerRegisteredEvent event) {
        this.customerId = event.getCustomerId();
        this.customerName = event.getCustomerName();
        this.email = event.getEmail();
        this.phoneNumber = event.getPhoneNumber();
        this.address = event.getAddress();
        this.paymentMethods = new ArrayList<>();
    }

    @EventSourcingHandler
    public void on(PaymentMethodAddedEvent event) {
        this.paymentMethods.add(event.getPaymentCard());
        if (event.isDefault()) {
            this.defaultPaymentMethod = event.getPaymentCard();
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    // Getters for testing and state verification
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public List<PaymentCard> getPaymentMethods() { return new ArrayList<>(paymentMethods); }
    public PaymentCard getDefaultPaymentMethod() { return defaultPaymentMethod; }
}