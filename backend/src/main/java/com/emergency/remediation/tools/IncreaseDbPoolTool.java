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
 * Increases the DB connection pool size to resolve pool exhaustion.
 * Rollback: restore previous pool size.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IncreaseDbPoolTool implements RemediationTool {

    private final SimulatedDatabaseState databaseState;
    private final ServiceRegistry serviceRegistry;

    @Override
    public RemediationActionType getType() { return RemediationActionType.INCREASE_DB_POOL; }

    @Override
    public RiskLevel getRiskLevel() { return RiskLevel.MEDIUM; }

    @Override
    public String getDescription() {
        return "Increase DB connection pool size to resolve exhaustion";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            int oldSize = databaseState.getPoolSize();
            int newSize = extractInt(parameters, "newPoolSize", "newSize", "poolSize");
            if (newSize <= 0) newSize = oldSize + 50;

            databaseState.setPoolSize(newSize);
            // Pool expansion immediately makes connections available
            // activeConnections stays the same (requests already in flight)
            serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.HEALTHY);
            serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.HEALTHY);

            log.info("DB pool expanded from {} to {}", oldSize, newSize);
            return String.format("DB pool size increased from %d to %d. Available connections now: %d.",
                    oldSize, newSize, databaseState.getAvailableConnections());
        } catch (Exception e) {
            log.error("IncreaseDbPoolTool failed", e);
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public Map<String, Object> compensationParameters(Map<String, Object> original) {
        return Map.of("previousPoolSize", databaseState.getPoolSize());
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
