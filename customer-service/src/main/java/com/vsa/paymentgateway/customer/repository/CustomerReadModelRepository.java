package com.vsa.paymentgateway.customer.repository;

import com.vsa.paymentgateway.customer.readmodel.CustomerReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Customer read model queries
 * Part of the Customer Onboarding vertical slice - CQRS read side
 */
@Repository
public interface CustomerReadModelRepository extends JpaRepository<CustomerReadModel, String> {

    Optional<CustomerReadModel> findByEmail(String email);
    
    @Query("SELECT c FROM CustomerReadModel c WHERE c.customerName LIKE %:name%")
    List<CustomerReadModel> findByCustomerNameContaining(@Param("name") String name);
    
    @Query("SELECT c FROM CustomerReadModel c JOIN FETCH c.paymentMethods WHERE c.customerId = :customerId")
    Optional<CustomerReadModel> findByIdWithPaymentMethods(@Param("customerId") String customerId);
    
    boolean existsByEmail(String email);
}