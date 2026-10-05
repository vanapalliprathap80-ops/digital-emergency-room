package com.emergency.remediation.tools;

import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RiskLevel;
import com.emergency.remediation.RemediationTool;
import com.emergency.service.HealthStatus;
import com.emergency.service.LogicalService;
import com.emergency.service.ServiceRegistry;
import com.emergency.service.SimulatedPaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestorePaymentTool implements RemediationTool {

    private final SimulatedPaymentState paymentState;
    private final ServiceRegistry serviceRegistry;

    @Override
    public RemediationActionType getType() { return RemediationActionType.RESTORE_PAYMENT_SERVICE; }

    @Override
    public RiskLevel getRiskLevel() { return RiskLevel.LOW; }

    @Override
    public String getDescription() { return "Restore payment service availability"; }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            paymentState.setAvailable(true);
            paymentState.setTimeoutEnabled(false);
            paymentState.setFailureRate(0.0);
            serviceRegistry.setState(LogicalService.PAYMENT, HealthStatus.HEALTHY);
            serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.HEALTHY);
            log.info("Payment service restored");
            return "Payment service restored to available. Timeout disabled, failure rate reset to 0.";
        } catch (Exception e) {
            log.error("RestorePaymentTool failed", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
