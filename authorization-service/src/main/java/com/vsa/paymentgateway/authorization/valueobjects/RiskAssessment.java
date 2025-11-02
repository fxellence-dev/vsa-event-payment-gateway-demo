package com.vsa.paymentgateway.authorization.valueobjects;

/**
 * Risk assessment result value object
 * Part of the Payment Authorization vertical slice
 */
public class RiskAssessment {
    
    private final boolean approved;
    private final String declineReason;
    private final String declineCode;
    private final int riskScore;
    private final String riskLevel;

    public RiskAssessment(boolean approved, String declineReason, String declineCode, 
                          int riskScore, String riskLevel) {
        this.approved = approved;
        this.declineReason = declineReason;
        this.declineCode = declineCode;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }

    public static RiskAssessment approved(int riskScore, String riskLevel) {
        return new RiskAssessment(true, null, null, riskScore, riskLevel);
    }

    public static RiskAssessment declined(String reason, String code, int riskScore, String riskLevel) {
        return new RiskAssessment(false, reason, code, riskScore, riskLevel);
    }

    public boolean isApproved() { return approved; }
    public String getDeclineReason() { return declineReason; }
    public String getDeclineCode() { return declineCode; }
    public int getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
}