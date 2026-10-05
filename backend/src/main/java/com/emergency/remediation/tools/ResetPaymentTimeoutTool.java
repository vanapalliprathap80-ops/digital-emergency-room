package com.emergency.remediation.tools;

import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RiskLevel;
import com.emergency.remediation.RemediationTool;
import com.emergency.service.SimulatedPaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResetPaymentTimeoutTool implements RemediationTool {

    private final SimulatedPaymentState paymentState;

    @Override
    public RemediationActionType getType() { return RemediationActionType.RESET_PAYMENT_TIMEOUT; }

    @Override
    public RiskLevel getRiskLevel() { return RiskLevel.LOW; }

    @Override
    public String getDescription() { return "Reset payment timeout to normal levels"; }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            long prevTimeout = paymentState.getTimeoutDurationMs();
            paymentState.setTimeoutEnabled(false);
            paymentState.setTimeoutDurationMs(8000);
            log.info("Payment timeout reset from {}ms", prevTimeout);
            return String.format("Payment timeout disabled (was %dms). Normal payment processing restored.", prevTimeout);
        } catch (Exception e) {
            log.error("ResetPaymentTimeoutTool failed", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
