package com.vsa.paymentgateway.settlement.queries;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Query service for Settlement operations
 * Provides convenient query methods for settlement data
 */
@Service
public class SettlementQueryService {
    
    private final SettlementRepository settlementRepository;
    
    public SettlementQueryService(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }
    
    /**
     * Find settlement by ID
     */
    public Optional<SettlementReadModel> findById(String settlementId) {
        return settlementRepository.findById(settlementId);
    }
    
    /**
     * Find settlement by payment ID
     */
    public Optional<SettlementReadModel> findByPaymentId(String paymentId) {
        return settlementRepository.findByPaymentId(paymentId);
    }
    
    /**
     * Find settlement by processing ID
     */
    public Optional<SettlementReadModel> findByProcessingId(String processingId) {
        return settlementRepository.findByProcessingId(processingId);
    }
    
    /**
     * Find all settlements for a merchant
     */
    public List<SettlementReadModel> findByMerchantId(String merchantId) {
        return settlementRepository.findByMerchantId(merchantId);
    }
    
    /**
     * Find all settlements by status
     */
    public List<SettlementReadModel> findByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status);
    }
    
    /**
     * Find all settlements in a batch
     */
    public List<SettlementReadModel> findByBatchId(String settlementBatchId) {
        return settlementRepository.findBySettlementBatchId(settlementBatchId);
    }
    
    /**
     * Get all pending settlements
     */
    public List<SettlementReadModel> findPendingSettlements() {
        return settlementRepository.findByStatus(SettlementStatus.PENDING);
    }
    
    /**
     * Get all failed settlements (for retry)
     */
    public List<SettlementReadModel> findFailedSettlements() {
        return settlementRepository.findByStatus(SettlementStatus.FAILED);
    }
}
