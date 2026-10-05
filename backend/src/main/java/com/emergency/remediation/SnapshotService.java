package com.emergency.remediation;

import com.emergency.metrics.MetricsService;
import com.emergency.service.SimulatedDatabaseState;
import com.emergency.service.SimulatedDeploymentState;
import com.emergency.service.SimulatedPaymentState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final SimulatedDatabaseState databaseState;
    private final SimulatedPaymentState paymentState;
    private final SimulatedDeploymentState deploymentState;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    /**
     * Captures a snapshot of the current simulator state and actual telemetry metrics.
     * Used for before/after comparison during remediation.
     */
    public String captureSnapshot() {
        try {
            Map<String, Object> snapshot = Map.of(
                    "simulator", Map.of(
                            "database", Map.of(
                                    "available", databaseState.isAvailable(),
                                    "poolSize", databaseState.getPoolSize(),
                                    "activeConnections", databaseState.getActiveConnections().get(),
                                    "acquisitionFailures", databaseState.getAcquisitionFailures().get(),
                                    "extraQueryLatencyMs", databaseState.getExtraQueryLatencyMs(),
                                    "queryFailureRate", databaseState.getQueryFailureRate()
                            ),
                            "payment", Map.of(
                                    "available", paymentState.isAvailable(),
                                    "timeoutEnabled", paymentState.isTimeoutEnabled(),
                                    "failureRate", paymentState.getFailureRate()
                            ),
                            "deployment", Map.of(
                                    "version", deploymentState.getVersion(),
                                    "degraded", deploymentState.isDegradedDeployment(),
                                    "failureRate", deploymentState.getFailureRate()
                            )
                    ),
                    "metrics", metricsService.getSystemMetrics()
            );
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("Failed to serialize snapshot", e);
            return "{}";
        }
    }
}
