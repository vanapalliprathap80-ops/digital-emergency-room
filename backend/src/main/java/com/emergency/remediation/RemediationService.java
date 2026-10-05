package com.emergency.remediation;

import com.emergency.domain.*;
import com.emergency.repository.IncidentRepository;
import com.emergency.repository.IncidentTimelineRepository;
import com.emergency.repository.RemediationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemediationService {

    private final RemediationRepository remediationRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository timelineRepository;
    private final com.emergency.repository.InvestigationRepository investigationRepository;
    private final com.emergency.repository.InvestigationEvidenceRepository investigationEvidenceRepository;
    private final SafetyGateway safetyGateway;
    private final RemediationToolRegistry toolRegistry;
    private final VerificationEngine verificationEngine;
    private final SnapshotService snapshotService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RemediationAction proposeAction(String incidentId, String investigationId, String actionType, String target,
                                           Map<String, Object> parameters, String reason, String expectedImpact) {

        String actionId = "rem-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Proposing remediation {} for incident {} with target {}", actionType, incidentId, target);

        timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.REMEDIATION_PROPOSED, null,
                "Remediation proposed: " + actionType, actionType));
                
        // Enforce Backend Invariants First
        boolean invariantPassed = true;
        String invariantError = null;

        if (target == null || target.trim().isEmpty() || target.equalsIgnoreCase("UNKNOWN")) {
            invariantPassed = false;
            invariantError = "Remediation target is unknown.";
        } else {
            var invOpt = investigationRepository.findById(investigationId);
            if (invOpt.isEmpty()) {
                invariantPassed = false;
                invariantError = "Investigation not found: " + investigationId;
            } else {
                var inv = invOpt.get();
                if (inv.getStatus() != InvestigationStatus.COMPLETED) {
                    invariantPassed = false;
                    invariantError = "Remediation requires a completed investigation.";
                } else if (inv.getDiagnosisStatus() != DiagnosisStatus.SUCCESS) {
                    invariantPassed = false;
                    invariantError = "Remediation requires a completed evidence-backed diagnosis.";
                } else if (inv.getRootCauseService() == null || inv.getRootCauseFailure() == null) {
                    invariantPassed = false;
                    invariantError = "Diagnosis is incomplete (missing service or failure type).";
                } else if (inv.getRecommendedAction() == null) {
                    invariantPassed = false;
                    invariantError = "No recommended action in diagnosis.";
                } else {
                    long evidenceCount = investigationEvidenceRepository.countByInvestigationId(investigationId);
                    if (evidenceCount == 0) {
                        invariantPassed = false;
                        invariantError = "Remediation requires a completed evidence-backed diagnosis.";
                    }
                }
            }
        }
        
        if (!invariantPassed) {
            log.error("Action blocked by backend invariant: {}", invariantError);
            RemediationAction action = new RemediationAction(actionId, incidentId, investigationId,
                    RemediationActionType.UNSUPPORTED, target != null ? target : "UNKNOWN", "{}", RiskLevel.CRITICAL, reason, expectedImpact, "SYSTEM");
            action.setStatus(RemediationStatus.BLOCKED);
            action.setFailureReason(invariantError);
            timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.SAFETY_CHECK_FAILED, null,
                    "Backend Invariant check failed: " + invariantError, null));
            timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.ACTION_BLOCKED, null,
                    "Action blocked by policy", null));
            return remediationRepository.save(action);
        }

        timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.SAFETY_CHECK_STARTED, null,
                "Validating proposal via Safety Gateway", null));

        SafetyValidationResult validation = safetyGateway.validate(incidentId, actionType, parameters);

        try {
            String validationJson = objectMapper.writeValueAsString(validation);

            RiskLevel riskLevel = validation.getRiskLevel() != null ? validation.getRiskLevel() : RiskLevel.CRITICAL;
            RemediationActionType resolvedType = validation.getResolvedActionType() != null ?
                    validation.getResolvedActionType() : RemediationActionType.UNSUPPORTED;

            RemediationAction action = new RemediationAction(actionId, incidentId, investigationId,
                    resolvedType, target, objectMapper.writeValueAsString(parameters),
                    riskLevel, reason, expectedImpact, "AI_AGENT");

            // Compute current attempt number (count previous actions for this incident)
            long attempts = remediationRepository.findByIncidentIdOrderByProposedAtDesc(incidentId).size();
            action.setAttemptNumber((int) attempts + 1);

            action.setValidationResult(validationJson);

            if (!validation.isPassed()) {
                action.setStatus(RemediationStatus.BLOCKED);
                action.setFailureReason(validation.getBlockedReason());
                timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.SAFETY_CHECK_FAILED, null,
                        "Safety check failed: " + validation.getBlockedReason(), null));
                timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.ACTION_BLOCKED, null,
                        "Action blocked by policy", null));
            } else if (validation.isRequiresApproval()) {
                action.setStatus(RemediationStatus.PENDING_APPROVAL);
                timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.SAFETY_CHECK_PASSED, null,
                        "Safety check passed. Risk Level: " + riskLevel, null));
                timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.APPROVAL_REQUESTED, null,
                        "Action requires human approval", null));
            } else {
                // Auto approve
                action.setStatus(RemediationStatus.APPROVED);
                action.setApprovedAt(Instant.now());
                action.setApprovedBy("SYSTEM_AUTO");
                timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.SAFETY_CHECK_PASSED, null,
                        "Safety check passed. Risk Level: " + riskLevel, null));
                timelineRepository.save(new IncidentTimelineEvent(incidentId, TimelineEventType.ACTION_APPROVED, null,
                        "Action auto-approved based on policy", null));
            }

            RemediationAction saved = remediationRepository.save(action);

            if (saved.getStatus() == RemediationStatus.APPROVED) {
                // Execute asynchronously
                executeAsync(saved.getActionId());
            }

            return saved;
        } catch (Exception e) {
            log.error("Failed to propose action", e);
            throw new RuntimeException("Failed to propose action", e);
        }
    }

    @Transactional
    public RemediationAction approveAction(String actionId, String approvedBy) {
        var action = remediationRepository.findById(actionId).orElseThrow();
        if (action.getStatus() != RemediationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Action is not pending approval");
        }

        action.setStatus(RemediationStatus.APPROVED);
        action.setApprovedAt(Instant.now());
        action.setApprovedBy(approvedBy);
        remediationRepository.save(action);

        timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.ACTION_APPROVED, null,
                "Action approved by " + approvedBy, null));

        executeAsync(actionId);
        return action;
    }

    @Transactional
    public RemediationAction rejectAction(String actionId, String rejectedBy, String reason) {
        var action = remediationRepository.findById(actionId).orElseThrow();
        action.setStatus(RemediationStatus.REJECTED);
        action.setFailureReason("Rejected by " + rejectedBy + ": " + reason);
        remediationRepository.save(action);

        timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.ACTION_REJECTED, null,
                "Action rejected by " + rejectedBy, null));
        return action;
    }

    @Async
    public void executeAsync(String actionId) {
        executeAction(actionId);
    }

    @Transactional
    public void executeAction(String actionId) {
        var action = remediationRepository.findById(actionId).orElseThrow();
        if (action.getStatus() != RemediationStatus.APPROVED) {
            return;
        }

        try {
            action.setStatus(RemediationStatus.EXECUTING);
            action.setBeforeSnapshot(snapshotService.captureSnapshot());
            remediationRepository.save(action);

            timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.ACTION_EXECUTING, null,
                    "Executing remediation: " + action.getActionType(), null));

            var incidentOpt = incidentRepository.findByIncidentId(action.getIncidentId());
            if (incidentOpt.isPresent()) {
                var inc = incidentOpt.get();
                if (inc.getStatus() == IncidentStatus.ACTIVE) {
                    inc.setStatus(IncidentStatus.RECOVERING);
                    incidentRepository.save(inc);
                }
            }

            Map<String, Object> params = objectMapper.readValue(action.getParameters(), Map.class);
            String result = toolRegistry.execute(action.getActionType(), params);

            if (result.startsWith("ERROR:")) {
                action.setStatus(RemediationStatus.FAILED);
                action.setFailureReason(result);
                action.setExecutedAt(Instant.now());
                remediationRepository.save(action);
                timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.ACTION_FAILED, null,
                        "Action execution failed: " + result, null));
            } else {
                action.setStatus(RemediationStatus.VERIFICATION_PENDING);
                action.setExecutionResult(result);
                action.setExecutedAt(Instant.now());
                remediationRepository.save(action);
                timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.ACTION_EXECUTED, null,
                        "Action executed successfully", result));

                // Proceed to verification
                verifyAsync(actionId);
            }
        } catch (Exception e) {
            log.error("Execution failed for {}", actionId, e);
            action.setStatus(RemediationStatus.FAILED);
            action.setFailureReason(e.getMessage());
            remediationRepository.save(action);
        }
    }

    @Async
    public void verifyAsync(String actionId) {
        verifyAction(actionId);
    }

    @Transactional
    public void verifyAction(String actionId) {
        var action = remediationRepository.findById(actionId).orElseThrow();
        if (action.getStatus() != RemediationStatus.VERIFICATION_PENDING) return;

        timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.VERIFICATION_STARTED, null,
                "Independent recovery verification started", null));

        boolean passed = verificationEngine.verify(action);
        action.setCompletedAt(Instant.now());

        if (passed) {
            action.setStatus(RemediationStatus.VERIFIED);
            timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.VERIFICATION_PASSED, null,
                    "Verification passed: System recovered", null));

            var inc = incidentRepository.findByIncidentId(action.getIncidentId()).orElseThrow();
            inc.setStatus(IncidentStatus.RESOLVED);
            inc.setEndedAt(Instant.now());
            incidentRepository.save(inc);

            timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.INCIDENT_RESOLVED, null,
                    "Incident resolved", null));
        } else {
            action.setStatus(RemediationStatus.VERIFICATION_FAILED);
            timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.VERIFICATION_FAILED, null,
                    "Verification failed: Symptoms persist", null));

            if (action.getAttemptNumber() >= 3) {
                action.setStatus(RemediationStatus.ESCALATED);
                timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.INCIDENT_ESCALATED, null,
                        "Incident escalated to human responders after 3 failed attempts", null));
                
                var inc = incidentRepository.findByIncidentId(action.getIncidentId()).orElseThrow();
                // We keep it ACTIVE or RECOVERING so UI knows it's broken
                inc.setStatus(IncidentStatus.ACTIVE);
                incidentRepository.save(inc);
            } else {
                timelineRepository.save(new IncidentTimelineEvent(action.getIncidentId(), TimelineEventType.REINVESTIGATION_STARTED, null,
                        "Re-investigation loop triggered", null));
                var inc = incidentRepository.findByIncidentId(action.getIncidentId()).orElseThrow();
                inc.setStatus(IncidentStatus.ACTIVE);
                incidentRepository.save(inc);
                // In a full implementation we would kick off the AgentOrchestrator again here.
            }
        }

        remediationRepository.save(action);
    }
}
