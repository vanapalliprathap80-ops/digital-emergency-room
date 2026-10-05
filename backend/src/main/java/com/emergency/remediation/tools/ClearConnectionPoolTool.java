package com.emergency.remediation.tools;

import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RiskLevel;
import com.emergency.remediation.RemediationTool;
import com.emergency.service.HealthStatus;
import com.emergency.service.LogicalService;
import com.emergency.service.ServiceRegistry;
import com.emergency.service.SimulatedDatabaseState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Clears all held connections and resets acquisition failure counter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClearConnectionPoolTool implements RemediationTool {

    private final SimulatedDatabaseState databaseState;
    private final ServiceRegistry serviceRegistry;

    @Override
    public RemediationActionType getType() { return RemediationActionType.CLEAR_CONNECTION_POOL; }

    @Override
    public RiskLevel getRiskLevel() { return RiskLevel.LOW; }

    @Override
    public String getDescription() {
        return "Clear held connections and reset acquisition failure counter";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            long prevFailures = databaseState.getAcquisitionFailures().get();
            int prevActive = databaseState.getActiveConnections().get();

            databaseState.getActiveConnections().set(0);
            databaseState.getAcquisitionFailures().set(0);
            serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.HEALTHY);

            log.info("Connection pool cleared. Prev active={}, failures={}", prevActive, prevFailures);
            return String.format("Connection pool cleared. Prev active connections: %d, acquisition failures: %d. Pool now fully available.",
                    prevActive, prevFailures);
        } catch (Exception e) {
            log.error("ClearConnectionPoolTool failed", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
