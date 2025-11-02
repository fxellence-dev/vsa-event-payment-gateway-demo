package com.vsa.paymentgateway.settlement.queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Settlement read models
 */
@Repository
public interface SettlementRepository extends JpaRepository<SettlementReadModel, String> {
    
    /**
     * Find settlement by payment ID
     */
    Optional<SettlementReadModel> findByPaymentId(String paymentId);
    
    /**
     * Find all settlements by merchant ID
     */
    List<SettlementReadModel> findByMerchantId(String merchantId);
    
    /**
     * Find all settlements by status
     */
    List<SettlementReadModel> findByStatus(SettlementStatus status);
    
    /**
     * Find all settlements in a batch
     */
    List<SettlementReadModel> findBySettlementBatchId(String settlementBatchId);
    
    /**
     * Find all settlements by processing ID
     */
    Optional<SettlementReadModel> findByProcessingId(String processingId);
}
