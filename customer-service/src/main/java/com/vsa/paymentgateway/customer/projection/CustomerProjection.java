package com.vsa.paymentgateway.customer.projection;

import com.vsa.paymentgateway.customer.events.CustomerRegisteredEvent;
import com.vsa.paymentgateway.customer.events.PaymentMethodAddedEvent;
import com.vsa.paymentgateway.customer.readmodel.CustomerReadModel;
import com.vsa.paymentgateway.customer.readmodel.PaymentMethodReadModel;
import com.vsa.paymentgateway.customer.repository.CustomerReadModelRepository;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Event projections to build read models from domain events
 * Part of the Customer Onboarding vertical slice - CQRS read side
 */
@Component
public class CustomerProjection {

    private final CustomerReadModelRepository customerRepository;

    public CustomerProjection(CustomerReadModelRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @EventHandler
    public void on(CustomerRegisteredEvent event) {
        // Idempotent: only create if doesn't exist
        if (!customerRepository.existsById(event.getCustomerId())) {
            CustomerReadModel customer = new CustomerReadModel(
                event.getCustomerId(),
                event.getCustomerName(),
                event.getEmail(),
                event.getPhoneNumber(),
                event.getAddress(),
                event.getTimestamp()
            );
            
            customerRepository.save(customer);
        }
    }

    @EventHandler
    public void on(PaymentMethodAddedEvent event) {
        Optional<CustomerReadModel> customerOpt = customerRepository.findById(event.getCustomerId());
        
        if (customerOpt.isPresent()) {
            CustomerReadModel customer = customerOpt.get();
            
            // If this is the default method, update existing default methods
            if (event.isDefault()) {
                customer.getPaymentMethods().forEach(pm -> pm.setDefault(false));
            }
            
            PaymentMethodReadModel paymentMethod = new PaymentMethodReadModel(
                customer,
                event.getPaymentCard().getMaskedCardNumber(),
                event.getPaymentCard().getCardType().toString(),
                event.getPaymentCard().getExpiryMonth(),
                event.getPaymentCard().getExpiryYear(),
                event.getPaymentCard().getCardHolderName(),
                event.isDefault(),
                event.getTimestamp()
            );
            
            customer.getPaymentMethods().add(paymentMethod);
            customerRepository.save(customer);
        }
    }
}