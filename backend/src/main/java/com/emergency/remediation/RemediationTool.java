package com.emergency.remediation;

import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RiskLevel;

import java.util.Map;

/**
 * Contract for all typed remediation tools.
 * Each tool corresponds to exactly one RemediationActionType.
 * Tools do NOT execute autonomously — they are called only after SafetyGateway approval.
 */
public interface RemediationTool {

    RemediationActionType getType();

    RiskLevel getRiskLevel();

    String getDescription();

    /**
     * Execute the remediation against the actual simulator state.
     * Returns a human-readable execution result summary.
     * Must never throw — catch all exceptions and return an error string.
     */
    String execute(Map<String, Object> parameters);

    /**
     * Optional: return compensation parameters that can undo this action.
     * Returns null if no deterministic rollback is available.
     */
    default Map<String, Object> compensationParameters(Map<String, Object> originalParameters) {
        return null;
    }
}
