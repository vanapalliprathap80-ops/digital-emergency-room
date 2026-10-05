package com.emergency.remediation.tools;

import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RiskLevel;
import com.emergency.remediation.RemediationTool;
import com.emergency.service.HealthStatus;
import com.emergency.service.LogicalService;
import com.emergency.service.ServiceRegistry;
import com.emergency.service.SimulatedDeploymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HIGH risk. Rolls back a degraded deployment to the last known good version.
 * Requires human approval before execution.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RollbackDeploymentTool implements RemediationTool {

    private final SimulatedDeploymentState deploymentState;
    private final ServiceRegistry serviceRegistry;

    @Override
    public RemediationActionType getType() { return RemediationActionType.ROLLBACK_DEPLOYMENT; }

    @Override
    public RiskLevel getRiskLevel() { return RiskLevel.HIGH; }

    @Override
    public String getDescription() { return "Rollback to the last known good deployment"; }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            String badVersion = deploymentState.getVersion();
            deploymentState.setVersion("v1.0.0");
            deploymentState.setDeploymentStatus("HEALTHY");
            deploymentState.setDegradedDeployment(false);
            deploymentState.setFailureRate(0.0);
            serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.HEALTHY);
            serviceRegistry.setState(LogicalService.API_GATEWAY, HealthStatus.HEALTHY);
            log.info("Deployment rolled back from {} to v1.0.0", badVersion);
            return String.format("Deployment rolled back from %s to v1.0.0. Service health restored to HEALTHY.", badVersion);
        } catch (Exception e) {
            log.error("RollbackDeploymentTool failed", e);
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public Map<String, Object> compensationParameters(Map<String, Object> original) {
        return Map.of("note", "Rollback compensation would require re-deploying the bad version — not supported.");
    }
}
