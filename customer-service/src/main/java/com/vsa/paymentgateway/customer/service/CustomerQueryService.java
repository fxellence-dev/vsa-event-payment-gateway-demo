package com.vsa.paymentgateway.customer.service;

import com.vsa.paymentgateway.customer.readmodel.CustomerReadModel;
import com.vsa.paymentgateway.customer.repository.CustomerReadModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Query service for customer-related read operations
 * Part of the Customer Onboarding vertical slice - CQRS read side
 */
@Service
public class CustomerQueryService {

    private final CustomerReadModelRepository customerRepository;

    public CustomerQueryService(CustomerReadModelRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<CustomerReadModel> findCustomerById(String customerId) {
        return customerRepository.findById(customerId);
    }

    public Optional<CustomerReadModel> findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public Optional<CustomerReadModel> findCustomerWithPaymentMethods(String customerId) {
        return customerRepository.findByIdWithPaymentMethods(customerId);
    }

    public List<CustomerReadModel> searchCustomersByName(String name) {
        return customerRepository.findByCustomerNameContaining(name);
    }

    public List<CustomerReadModel> findAllCustomers() {
        return customerRepository.findAll();
    }

    public boolean customerExistsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }
}