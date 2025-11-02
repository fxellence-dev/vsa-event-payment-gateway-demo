package com.vsa.paymentgateway.authorization.service;

import com.vsa.paymentgateway.authorization.valueobjects.RiskAssessment;
import com.vsa.paymentgateway.common.valueobjects.Money;
import com.vsa.paymentgateway.common.valueobjects.PaymentCard;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Risk assessment service for payment authorization
 * Part of the Payment Authorization vertical slice
 * 
 * This service simulates real-world risk assessment including:
 * - Amount-based risk scoring
 * - Card type validation
 * - Fraud detection patterns
 * - Merchant risk assessment
 */
@Service
public class RiskAssessmentService {

    private final Random random = new Random();

    public RiskAssessment assessRisk(String customerId, Money amount, PaymentCard paymentCard, String merchantId) {
        int riskScore = calculateRiskScore(amount, paymentCard, merchantId);
        String riskLevel = determineRiskLevel(riskScore);
        
        // Determine if transaction should be approved
        if (riskScore > 80) {
            return RiskAssessment.declined("High risk transaction", "RISK_HIGH", riskScore, riskLevel);
        }
        
        if (amount.getAmountAsDouble() > 5000 && riskScore > 60) {
            return RiskAssessment.declined("Large amount with elevated risk", "RISK_AMOUNT", riskScore, riskLevel);
        }
        
        // Simulate card-specific declines
        if (paymentCard.getCardType() == PaymentCard.CardType.UNKNOWN) {
            return RiskAssessment.declined("Unsupported card type", "CARD_UNSUPPORTED", riskScore, riskLevel);
        }
        
        // Simulate random declines for demonstration (5% decline rate)
        if (random.nextInt(100) < 5) {
            return RiskAssessment.declined("Issuer declined", "ISSUER_DECLINE", riskScore, riskLevel);
        }
        
        return RiskAssessment.approved(riskScore, riskLevel);
    }

    private int calculateRiskScore(Money amount, PaymentCard paymentCard, String merchantId) {
        int score = 0;
        
        // Amount-based scoring
        double amountValue = amount.getAmountAsDouble();
        if (amountValue > 1000) score += 20;
        if (amountValue > 5000) score += 30;
        if (amountValue > 10000) score += 50;
        
        // Card type scoring
        switch (paymentCard.getCardType()) {
            case VISA:
            case MASTERCARD:
                score += 10;
                break;
            case AMEX:
                score += 15;
                break;
            case UNKNOWN:
                score += 40;
                break;
        }
        
        // Merchant risk (simulated)
        if (merchantId.contains("high-risk")) {
            score += 30;
        }
        
        // Add some randomness to simulate real-world variability
        score += random.nextInt(20);
        
        return Math.min(score, 100);
    }

    private String determineRiskLevel(int riskScore) {
        if (riskScore < 30) return "LOW";
        if (riskScore < 60) return "MEDIUM";
        if (riskScore < 80) return "HIGH";
        return "VERY_HIGH";
    }
}