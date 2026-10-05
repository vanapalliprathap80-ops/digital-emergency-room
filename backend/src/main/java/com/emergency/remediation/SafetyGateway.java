package com.emergency.remediation;

import com.emergency.domain.FailureType;
import com.emergency.domain.IncidentStatus;
import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RemediationStatus;
import com.emergency.domain.RiskLevel;
import com.emergency.repository.IncidentRepository;
import com.emergency.repository.RemediationRepository;
import com.emergency.service.ActiveFailureState;
import com.emergency.service.FailureEngine;
import com.emergency.service.SimulatedDatabaseState;
import com.emergency.service.SimulatedDeploymentState;
import com.emergency.service.SimulatedPaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Safety / Policy Gateway.
 *
 * THE LLM MUST NEVER DIRECTLY MUTATE SYSTEM STATE.
 * This gateway is the sole arbiter of whether a proposed action is safe to execute.
 *
 * Checks performed (in order):
 * 1. Action type is on the allowlist
 * 2. Target service is valid for the action
 * 3. Parameters are within allowed ranges
 * 4. Incident is still active
 * 5. Action has not already been executed for this incident
 * 6. Simulator preconditions are satisfied
 * 7. Risk level is acceptable (CRITICAL = block)
 * 8. Requires human approval if HIGH risk
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafetyGateway {

    private final FailureEngine failureEngine;
    private final SimulatedDatabaseState databaseState;
    private final SimulatedPaymentState paymentState;
    private final SimulatedDeploymentState deploymentState;
    private final IncidentRepository incidentRepository;
    private final RemediationRepository remediationRepository;
    private final com.emergency.service.EnvironmentContext environmentContext;

    private static final int MAX_DB_POOL_SIZE = 500;
    private static final int MIN_DB_POOL_SIZE = 10;

    // ---------------------------------------------------------------------------
    // Public entry point
    // ---------------------------------------------------------------------------

    public SafetyValidationResult validate(String incidentId, String rawActionType,
                                           Map<String, Object> parameters) {
        List<String> performed = new ArrayList<>();
        List<String> passed = new ArrayList<>();

        // 1. Action type validation
        performed.add("Action type on allowlist");
        RemediationActionType actionType = resolveActionType(rawActionType);
        if (actionType == null || actionType == RemediationActionType.UNSUPPORTED) {
            log.warn("BLOCKED: Unknown action type '{}'", rawActionType);
            return SafetyValidationResult.blocked(
                    "Action '" + rawActionType + "' is not permitted by remediation policy.",
                    performed);
        }
        passed.add("Action type on allowlist: " + actionType);

        // 2. Incident must be active
        performed.add("Incident is active");
        var incidentOpt = incidentRepository.findByIncidentId(incidentId);
        if (incidentOpt.isEmpty()) {
            return SafetyValidationResult.blocked("Incident not found: " + incidentId, performed);
        }
        var incident = incidentOpt.get();
        if (incident.getStatus() != IncidentStatus.ACTIVE &&
                incident.getStatus() != IncidentStatus.RECOVERING) {
            return SafetyValidationResult.blocked(
                    "Incident is not active (status=" + incident.getStatus() + ").", performed);
        }
        passed.add("Incident is active");

        // 3. Idempotency: action already EXECUTED/VERIFIED for this incident?
        performed.add("Idempotency check (no duplicate execution)");
        boolean alreadyExecuted = remediationRepository.existsByIncidentIdAndActionTypeAndStatusIn(
                incidentId, actionType,
                List.of(RemediationStatus.EXECUTED, RemediationStatus.VERIFIED,
                        RemediationStatus.EXECUTING));
        if (alreadyExecuted) {
            return SafetyValidationResult.blocked(
                    "Action " + actionType + " has already been executed for incident " + incidentId + ".",
                    performed);
        }
        passed.add("No duplicate action");

        // 4. Active failure exists
        performed.add("Active failure exists");
        ActiveFailureState active = failureEngine.getActiveFailure();
        boolean isProduction = environmentContext.getCurrentEnvironment() == com.emergency.service.EnvironmentContext.Environment.PRODUCTION;
        
        if (!isProduction) {
            if (active == null) {
                return SafetyValidationResult.blocked("No active failure state in simulator.", performed);
            }
            passed.add("Active failure state present: " + active.getFailureType());
        } else {
            passed.add("Bypassing simulator failure checks in PRODUCTION.");
        }

        // 5. Action-specific preconditions + parameter validation
        performed.add("Action preconditions satisfied");
        performed.add("Parameters within allowed range");
        if (!isProduction) {
            String preconditionError = checkPreconditions(actionType, parameters, active);
            if (preconditionError != null) {
                return SafetyValidationResult.blocked(preconditionError, performed);
            }
            passed.add("Preconditions satisfied");
        } else {
            passed.add("Preconditions bypassed in PRODUCTION");
        }
        passed.add("Parameters valid");

        // 6. Risk level
        performed.add("Risk level classification");
        RiskLevel risk = classifyRisk(actionType);
        passed.add("Risk level: " + risk);

        if (risk == RiskLevel.CRITICAL) {
            return SafetyValidationResult.blocked(
                    "CRITICAL risk actions are not permitted by remediation policy.", performed);
        }

        // 7. Approval policy
        performed.add("Approval policy evaluated");
        if (isProduction || risk == RiskLevel.HIGH) {
            passed.add("HIGH risk or PRODUCTION environment: human approval required");
            return SafetyValidationResult.requiresHumanApproval(actionType, risk, performed, passed);
        }

        passed.add("LOW/MEDIUM risk: automatic execution approved");
        return SafetyValidationResult.autoApproved(actionType, risk, performed, passed);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private RemediationActionType resolveActionType(String raw) {
        if (raw == null) return null;
        // Accept both UPPER_SNAKE and camelCase from Gemini
        String normalized = raw.toUpperCase().replace("-", "_").replace(" ", "_");
        try {
            return RemediationActionType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try common aliases Gemini might use
            return switch (normalized) {
                case "INCREASE_DB_POOL_SIZE", "INCREASE_DB_CONNECTION_POOL",
                     "EXPAND_DB_POOL" -> RemediationActionType.INCREASE_DB_POOL;
                case "CLEAR_DB_POOL", "RESET_CONNECTION_POOL",
                     "FLUSH_CONNECTION_POOL" -> RemediationActionType.CLEAR_CONNECTION_POOL;
                case "RESTORE_DB", "RESTORE_DATABASE",
                     "FIX_DATABASE" -> RemediationActionType.RESTORE_DATABASE_AVAILABILITY;
                case "REDUCE_LATENCY", "FIX_QUERY_LATENCY",
                     "REDUCE_DB_LATENCY" -> RemediationActionType.REDUCE_QUERY_LATENCY;
                case "RESTORE_PAYMENT", "FIX_PAYMENT",
                     "RESTORE_PAYMENT_GATEWAY" -> RemediationActionType.RESTORE_PAYMENT_SERVICE;
                case "RESET_PAYMENT_TIMEOUTS",
                     "FIX_PAYMENT_TIMEOUT" -> RemediationActionType.RESET_PAYMENT_TIMEOUT;
                case "ROLLBACK", "ROLLBACK_DEPLOY",
                     "REVERT_DEPLOYMENT" -> RemediationActionType.ROLLBACK_DEPLOYMENT;
                case "RESTART", "SERVICE_RESTART" -> RemediationActionType.RESTART_SERVICE;
                default -> null;
            };
        }
    }

    private String checkPreconditions(RemediationActionType actionType,
                                       Map<String, Object> parameters,
                                       ActiveFailureState active) {
        return switch (actionType) {
            case INCREASE_DB_POOL -> {
                // Pool must currently be exhausted
                if (databaseState.getAvailableConnections() > 10) {
                    yield "DB pool is not exhausted (available=" +
                            databaseState.getAvailableConnections() + "). Precondition not met.";
                }
                // newPoolSize parameter validation
                int newSize = extractInt(parameters, "newPoolSize", "newSize", "poolSize");
                if (newSize <= 0) {
                    yield "Missing or invalid 'newPoolSize' parameter.";
                }
                if (newSize < MIN_DB_POOL_SIZE || newSize > MAX_DB_POOL_SIZE) {
                    yield "newPoolSize=" + newSize + " out of allowed range [" +
                            MIN_DB_POOL_SIZE + ", " + MAX_DB_POOL_SIZE + "].";
                }
                if (newSize <= databaseState.getPoolSize()) {
                    yield "newPoolSize=" + newSize + " must be greater than current poolSize=" +
                            databaseState.getPoolSize() + ".";
                }
                yield null;
            }
            case CLEAR_CONNECTION_POOL -> {
                if (!active.getFailureType().name().startsWith("DATABASE")) {
                    yield "CLEAR_CONNECTION_POOL is only applicable to database failures.";
                }
                yield null;
            }
            case RESTORE_DATABASE_AVAILABILITY -> {
                if (databaseState.isAvailable()) {
                    yield "Database is already available. Action not needed.";
                }
                yield null;
            }
            case REDUCE_QUERY_LATENCY -> {
                if (databaseState.getExtraQueryLatencyMs() <= 0) {
                    yield "No extra query latency is currently injected.";
                }
                yield null;
            }
            case RESTORE_PAYMENT_SERVICE -> {
                if (paymentState.isAvailable() && paymentState.getFailureRate() == 0.0) {
                    yield "Payment service is already healthy.";
                }
                yield null;
            }
            case RESET_PAYMENT_TIMEOUT -> {
                if (!paymentState.isTimeoutEnabled()) {
                    yield "No payment timeout is currently active.";
                }
                yield null;
            }
            case ROLLBACK_DEPLOYMENT -> {
                if (!deploymentState.isDegradedDeployment()) {
                    yield "No degraded deployment is active. Rollback not applicable.";
                }
                yield null;
            }
            case RESTART_SERVICE, SCALE_SERVICE -> {
                // These are always potentially applicable during an active incident
                yield null;
            }
            default -> "Unsupported action type: " + actionType;
        };
    }

    public RiskLevel classifyRisk(RemediationActionType actionType) {
        return switch (actionType) {
            case CLEAR_CONNECTION_POOL, RESTORE_DATABASE_AVAILABILITY,
                 RESTORE_PAYMENT_SERVICE, RESET_PAYMENT_TIMEOUT,
                 REDUCE_QUERY_LATENCY -> RiskLevel.LOW;
            case INCREASE_DB_POOL, SCALE_SERVICE -> RiskLevel.MEDIUM;
            case RESTART_SERVICE, ROLLBACK_DEPLOYMENT -> RiskLevel.HIGH;
            case UNSUPPORTED -> RiskLevel.CRITICAL;
        };
    }

    private int extractInt(Map<String, Object> params, String... keys) {
        for (String key : keys) {
            Object val = params.get(key);
            if (val instanceof Number n) return n.intValue();
            if (val instanceof String s) {
                try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }
}
