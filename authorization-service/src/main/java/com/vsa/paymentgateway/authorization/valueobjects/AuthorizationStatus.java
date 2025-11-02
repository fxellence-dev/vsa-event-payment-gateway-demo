package com.vsa.paymentgateway.authorization.valueobjects;

/**
 * Authorization status enumeration
 * Part of the Payment Authorization vertical slice
 */
public enum AuthorizationStatus {
    PENDING,
    AUTHORIZED,
    DECLINED,
    EXPIRED,
    CAPTURED,
    CANCELLED,
    VOIDED        // Added for compensation flow
}