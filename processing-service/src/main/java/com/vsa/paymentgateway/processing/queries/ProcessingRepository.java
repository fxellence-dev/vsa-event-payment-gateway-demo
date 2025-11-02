package com.vsa.paymentgateway.processing.queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProcessingReadModel
 */
@Repository
public interface ProcessingRepository extends JpaRepository<ProcessingReadModel, String> {
    
    /**
     * Find processing record by payment ID
     */
    Optional<ProcessingReadModel> findByPaymentId(String paymentId);
    
    /**
     * Find processing record by authorization ID
     */
    Optional<ProcessingReadModel> findByAuthorizationId(String authorizationId);
    
    /**
     * Find all processing records for a customer
     */
    List<ProcessingReadModel> findByCustomerId(String customerId);
    
    /**
     * Find processing records by status
     */
    List<ProcessingReadModel> findByStatus(ProcessingStatus status);
    
    /**
     * Find processing record by processor transaction ID
     */
    Optional<ProcessingReadModel> findByProcessorTransactionId(String processorTransactionId);
}
