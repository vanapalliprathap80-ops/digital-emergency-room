package com.emergency.remediation.tools;

import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RiskLevel;
import com.emergency.remediation.RemediationTool;
import com.emergency.service.SimulatedDatabaseState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReduceQueryLatencyTool implements RemediationTool {

    private final SimulatedDatabaseState databaseState;

    @Override
    public RemediationActionType getType() { return RemediationActionType.REDUCE_QUERY_LATENCY; }

    @Override
    public RiskLevel getRiskLevel() { return RiskLevel.LOW; }

    @Override
    public String getDescription() { return "Remove injected extra query latency from database"; }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            long prev = databaseState.getExtraQueryLatencyMs();
            databaseState.setExtraQueryLatencyMs(0);
            databaseState.setQueryFailureRate(0.0);
            log.info("Query latency reduced: {}ms -> 0ms", prev);
            return String.format("Extra query latency removed (was %dms). Query failure rate reset to 0.", prev);
        } catch (Exception e) {
            log.error("ReduceQueryLatencyTool failed", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
