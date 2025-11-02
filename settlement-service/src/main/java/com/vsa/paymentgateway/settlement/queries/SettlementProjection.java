package com.vsa.paymentgateway.settlement.queries;

import com.vsa.paymentgateway.settlement.events.PaymentSettledEvent;
import com.vsa.paymentgateway.settlement.events.SettlementFailedEvent;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Projection for Settlement events
 * Updates read models based on settlement events
 */
@Component
public class SettlementProjection {
    
    private static final Logger logger = LoggerFactory.getLogger(SettlementProjection.class);
    private final SettlementRepository settlementRepository;
    
    public SettlementProjection(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }
    
    /**
     * Handle PaymentSettledEvent
     * Updates read model to SETTLED status
     */
    @EventHandler
    public void on(PaymentSettledEvent event) {
        logger.info("Projecting PaymentSettledEvent: settlement={}, payment={}", 
                   event.getSettlementId(), event.getPaymentId());
        
        // Create or update read model
        SettlementReadModel model = settlementRepository.findById(event.getSettlementId())
            .orElseGet(() -> new SettlementReadModel(
                event.getSettlementId(),
                event.getPaymentId(),
                event.getProcessingId(),
                event.getAuthorizationId(),
                event.getMerchantId(),
                event.getAmount(),
                event.getCurrency()
            ));
        
        model.setStatus(SettlementStatus.SETTLED);
        model.setFeeAmount(event.getFeeAmount());
        model.setNetAmount(event.getNetAmount());
        model.setSettlementBatchId(event.getSettlementBatchId());
        model.setSettledAt(event.getSettledAt());
        
        settlementRepository.save(model);
        
        logger.debug("Settlement read model updated: {}", event.getSettlementId());
    }
    
    /**
     * Handle SettlementFailedEvent
     * Updates read model to FAILED status
     */
    @EventHandler
    public void on(SettlementFailedEvent event) {
        logger.info("Projecting SettlementFailedEvent: settlement={}, payment={}, reason={}", 
                   event.getSettlementId(), event.getPaymentId(), event.getFailureReason());
        
        // Create or update read model
        SettlementReadModel model = settlementRepository.findById(event.getSettlementId())
            .orElseGet(() -> new SettlementReadModel(
                event.getSettlementId(),
                event.getPaymentId(),
                event.getProcessingId(),
                null, // authorizationId not in failed event
                null, // merchantId not in failed event
                event.getAmount(),
                event.getCurrency()
            ));
        
        model.setStatus(SettlementStatus.FAILED);
        model.setFailureReason(event.getFailureReason());
        model.setErrorCode(event.getErrorCode());
        model.setFailedAt(event.getFailedAt());
        
        settlementRepository.save(model);
        
        logger.debug("Settlement read model updated (failed): {}", event.getSettlementId());
    }
}
