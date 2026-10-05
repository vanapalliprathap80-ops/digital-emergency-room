package com.emergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "remediation_actions")
@Getter
@Setter
@NoArgsConstructor
public class RemediationAction {

    @Id
    @Column(name = "action_id", length = 50)
    private String actionId;

    @Column(name = "incident_id", nullable = false, length = 50)
    private String incidentId;

    @Column(name = "investigation_id", length = 50)
    private String investigationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private RemediationActionType actionType;

    @Column(name = "target_service", length = 50)
    private String targetService;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RemediationStatus status;

    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "proposed_by", length = 50)
    private String proposedBy;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    /** JSON blob of safety-check results */
    @Column(name = "validation_result", columnDefinition = "TEXT")
    private String validationResult;

    /** JSON snapshot of key metrics before remediation */
    @Column(name = "before_snapshot", columnDefinition = "TEXT")
    private String beforeSnapshot;

    /** JSON snapshot of key metrics after remediation */
    @Column(name = "after_snapshot", columnDefinition = "TEXT")
    private String afterSnapshot;

    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "expected_impact", columnDefinition = "TEXT")
    private String expectedImpact;

    @Column(name = "rollback_available")
    private boolean rollbackAvailable;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "verification_result", columnDefinition = "TEXT")
    private String verificationResult;

    public RemediationAction(String actionId, String incidentId, String investigationId,
                              RemediationActionType actionType, String targetService,
                              String parameters, RiskLevel riskLevel,
                              String reason, String expectedImpact,
                              String proposedBy) {
        this.actionId = actionId;
        this.incidentId = incidentId;
        this.investigationId = investigationId;
        this.actionType = actionType;
        this.targetService = targetService;
        this.parameters = parameters;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.expectedImpact = expectedImpact;
        this.proposedBy = proposedBy;
        this.status = RemediationStatus.PROPOSED;
        this.proposedAt = Instant.now();
        this.rollbackAvailable = false;
    }
}
