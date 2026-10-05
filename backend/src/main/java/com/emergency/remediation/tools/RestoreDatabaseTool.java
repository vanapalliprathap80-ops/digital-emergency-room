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

@Component
@RequiredArgsConstructor
@Slf4j
public class RestoreDatabaseTool implements RemediationTool {

    private final SimulatedDatabaseState databaseState;
    private final ServiceRegistry serviceRegistry;

    @Override
    public RemediationActionType getType() { return RemediationActionType.RESTORE_DATABASE_AVAILABILITY; }

    @Override
    public RiskLevel getRiskLevel() { return RiskLevel.LOW; }

    @Override
    public String getDescription() { return "Restore database availability after outage"; }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            databaseState.setAvailable(true);
            databaseState.getAcquisitionFailures().set(0);
            serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.HEALTHY);
            serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.HEALTHY);
            log.info("Database availability restored");
            return "Database marked as available. Service health restored to HEALTHY.";
        } catch (Exception e) {
            log.error("RestoreDatabaseTool failed", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
