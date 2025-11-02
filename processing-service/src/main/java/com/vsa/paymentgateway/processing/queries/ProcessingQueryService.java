package com.vsa.paymentgateway.processing.queries;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Query service for processing operations
 */
@Service
public class ProcessingQueryService {
    
    private final ProcessingRepository repository;
    
    public ProcessingQueryService(ProcessingRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Find processing record by processing ID
     */
    public Optional<ProcessingReadModel> findById(String processingId) {
        return repository.findById(processingId);
    }
    
    /**
     * Find processing record by payment ID
     */
    public Optional<ProcessingReadModel> findByPaymentId(String paymentId) {
        return repository.findByPaymentId(paymentId);
    }
    
    /**
     * Find processing record by authorization ID
     */
    public Optional<ProcessingReadModel> findByAuthorizationId(String authorizationId) {
        return repository.findByAuthorizationId(authorizationId);
    }
    
    /**
     * Find all processing records for a customer
     */
    public List<ProcessingReadModel> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId);
    }
    
    /**
     * Find processing records by status
     */
    public List<ProcessingReadModel> findByStatus(ProcessingStatus status) {
        return repository.findByStatus(status);
    }
    
    /**
     * Find all pending processing records
     */
    public List<ProcessingReadModel> findPending() {
        return repository.findByStatus(ProcessingStatus.PENDING);
    }
    
    /**
     * Find all failed processing records
     */
    public List<ProcessingReadModel> findFailed() {
        return repository.findByStatus(ProcessingStatus.FAILED);
    }
}
