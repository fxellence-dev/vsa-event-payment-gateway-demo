package com.vsa.paymentgateway.processing.queries;

import com.vsa.paymentgateway.processing.events.PaymentProcessedEvent;
import com.vsa.paymentgateway.processing.events.PaymentProcessingFailedEvent;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Event handler that projects processing events into the read model
 */
@Component
public class ProcessingProjection {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessingProjection.class);
    
    private final ProcessingRepository repository;
    
    public ProcessingProjection(ProcessingRepository repository) {
        this.repository = repository;
    }
    
    @EventHandler
    public void on(PaymentProcessedEvent event) {
        logger.info("Projecting PaymentProcessedEvent for processing: {}", event.getProcessingId());
        
        ProcessingReadModel model = repository.findById(event.getProcessingId())
            .orElseGet(() -> {
                ProcessingReadModel newModel = new ProcessingReadModel(
                    event.getProcessingId(),
                    event.getPaymentId(),
                    event.getAuthorizationId(),
                    null, // customerId not in event
                    null, // paymentMethodId not in event
                    event.getAmount(),
                    event.getCurrency()
                );
                return newModel;
            });
        
        model.setStatus(ProcessingStatus.PROCESSED);
        model.setProcessorTransactionId(event.getProcessorTransactionId());
        model.setProcessedAt(event.getProcessedAt());
        
        repository.save(model);
        logger.debug("Processing read model updated: {} - PROCESSED", event.getProcessingId());
    }
    
    @EventHandler
    public void on(PaymentProcessingFailedEvent event) {
        logger.info("Projecting PaymentProcessingFailedEvent for processing: {}", event.getProcessingId());
        
        ProcessingReadModel model = repository.findById(event.getProcessingId())
            .orElseGet(() -> {
                ProcessingReadModel newModel = new ProcessingReadModel(
                    event.getProcessingId(),
                    event.getPaymentId(),
                    event.getAuthorizationId(),
                    null, // customerId not in event
                    null, // paymentMethodId not in event
                    event.getAmount(),
                    event.getCurrency()
                );
                return newModel;
            });
        
        model.setStatus(ProcessingStatus.FAILED);
        model.setFailureReason(event.getFailureReason());
        model.setErrorCode(event.getProcessorErrorCode());
        model.setFailedAt(event.getFailedAt());
        
        repository.save(model);
        logger.debug("Processing read model updated: {} - FAILED: {}", 
                    event.getProcessingId(), event.getFailureReason());
    }
}
