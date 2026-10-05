package com.emergency.remediation;

import com.emergency.domain.RemediationAction;
import com.emergency.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationEngine {

    private final MetricsService metricsService;
    private final SnapshotService snapshotService;

    /**
     * Independent Recovery Verification.
     * Uses fresh telemetry from AFTER the action is executed to determine success.
     */
    public boolean verify(RemediationAction action) {
        log.info("Verifying action: {}", action.getActionId());

        // Wait a short duration to allow fresh telemetry to accumulate in the simulator
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}

        var freshMetrics = metricsService.getSystemMetrics();

        // 1. Snapshot the "after" state
        action.setAfterSnapshot(snapshotService.captureSnapshot());

        // 2. Failure-specific recovery criteria
        boolean passed = switch (action.getActionType()) {
            case INCREASE_DB_POOL, CLEAR_CONNECTION_POOL,
                 RESTORE_DATABASE_AVAILABILITY, REDUCE_QUERY_LATENCY -> {
                var dbMetrics = metricsService.getServiceMetrics(com.emergency.service.LogicalService.DATABASE);
                yield dbMetrics.errorRate() < 5.0 && dbMetrics.p95LatencyMs() < 1000;
            }
            case RESTORE_PAYMENT_SERVICE, RESET_PAYMENT_TIMEOUT -> {
                var payMetrics = metricsService.getServiceMetrics(com.emergency.service.LogicalService.PAYMENT);
                yield payMetrics.errorRate() < 5.0 && payMetrics.p95LatencyMs() < 2000;
            }
            case ROLLBACK_DEPLOYMENT, RESTART_SERVICE, SCALE_SERVICE -> {
                yield freshMetrics.errorRate() < 5.0;
            }
            default -> false;
        };

        if (passed) {
            log.info("Verification PASSED for action {}", action.getActionId());
            action.setVerificationResult("PASSED: Target service error rates and latency returned to normal.");
        } else {
            log.warn("Verification FAILED for action {}. Fresh system error rate: {}",
                    action.getActionId(), freshMetrics.errorRate());
            action.setVerificationResult("FAILED: System metrics still showing elevated errors/latency.");
        }

        return passed;
    }
}
