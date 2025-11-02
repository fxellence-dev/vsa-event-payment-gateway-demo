package com.vsa.paymentgateway.settlement.queries;

/**
 * Settlement status enumeration for read model
 */
public enum SettlementStatus {
    PENDING,      // Settlement command created, not yet attempted
    SETTLING,     // Settlement in progress
    SETTLED,      // Successfully settled to merchant
    FAILED        // Settlement failed
}
