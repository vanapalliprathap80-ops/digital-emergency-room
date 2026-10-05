package com.emergency.remediation;

import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SafetyValidationResult {

    private final boolean passed;
    private final boolean requiresApproval;
    private final String blockedReason;
    private final List<String> checksPerformed;
    private final List<String> checksPassed;
    private final List<String> checksFailed;
    private final RemediationActionType resolvedActionType;
    private final RiskLevel riskLevel;

    public static SafetyValidationResult blocked(String reason, List<String> performed) {
        return SafetyValidationResult.builder()
                .passed(false)
                .requiresApproval(false)
                .blockedReason(reason)
                .checksPerformed(performed)
                .checksPassed(List.of())
                .checksFailed(List.of(reason))
                .build();
    }

    public static SafetyValidationResult autoApproved(RemediationActionType type, RiskLevel risk,
                                                      List<String> performed, List<String> passed) {
        return SafetyValidationResult.builder()
                .passed(true)
                .requiresApproval(false)
                .resolvedActionType(type)
                .riskLevel(risk)
                .checksPerformed(performed)
                .checksPassed(passed)
                .checksFailed(List.of())
                .build();
    }

    public static SafetyValidationResult requiresHumanApproval(RemediationActionType type, RiskLevel risk,
                                                               List<String> performed, List<String> passed) {
        return SafetyValidationResult.builder()
                .passed(true)
                .requiresApproval(true)
                .resolvedActionType(type)
                .riskLevel(risk)
                .checksPerformed(performed)
                .checksPassed(passed)
                .checksFailed(List.of())
                .build();
    }
}
