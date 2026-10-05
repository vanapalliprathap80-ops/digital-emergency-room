package com.emergency.service;

import com.emergency.events.EventPublisher;
import com.emergency.events.EventType;
import com.emergency.exception.SimulatorException;
import com.emergency.telemetry.TelemetryRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final TelemetryRecorder telemetryRecorder;
    private final EventPublisher eventPublisher;
    private final SimulatedPaymentState paymentState;
    private final SimulatedDatabaseState databaseState;
    private final SymptomLogger symptomLogger;
    private final Random random = new Random();

    public PaymentService(TelemetryRecorder telemetryRecorder,
                          EventPublisher eventPublisher,
                          SimulatedPaymentState paymentState,
                          SimulatedDatabaseState databaseState,
                          SymptomLogger symptomLogger) {
        this.telemetryRecorder = telemetryRecorder;
        this.eventPublisher = eventPublisher;
        this.paymentState = paymentState;
        this.databaseState = databaseState;
        this.symptomLogger = symptomLogger;
    }

    public void processPayment(String requestId, String orderId, String userId, double amount) {
        log.debug("Processing payment orderId={} requestId={}", orderId, requestId);

        // 1. Payment service availability check
        if (!paymentState.isAvailable()) {
            telemetryRecorder.record(requestId, LogicalService.PAYMENT.name(), "PROCESS_PAYMENT",
                    "/internal/payment", "POST", 503, 10, "SERVICE_UNAVAILABLE", "Payment microservice unavailable");
            symptomLogger.error(LogicalService.PAYMENT, "Payment endpoint connection refused: 503 Service Unavailable", requestId);
            throw new SimulatorException("Payment service unavailable");
        }

        // 2. Payment timeout check
        if (paymentState.isTimeoutEnabled()) {
            long timeout = paymentState.getTimeoutDurationMs();
            telemetryRecorder.record(requestId, LogicalService.PAYMENT.name(), "PROCESS_PAYMENT",
                    "/internal/payment", "POST", 504, timeout, "GATEWAY_TIMEOUT", "Upstream processor timed out");
            symptomLogger.error(LogicalService.PAYMENT,
                    "SocketTimeoutException after " + timeout + "ms while contacting third-party gateway", requestId);
            throw new SimulatorException("Payment gateway timeout");
        }

        // 3. Payment failure rate check
        if (random.nextDouble() < paymentState.getFailureRate()) {
            long paymentLatency = TelemetryRecorder.simulateLatency(30, 80);
            telemetryRecorder.record(requestId, LogicalService.PAYMENT.name(), "PROCESS_PAYMENT",
                    "/internal/payment", "POST", 402, paymentLatency, "TRANSACTION_DECLINED", "Card declined by processor");
            symptomLogger.warn(LogicalService.PAYMENT, "Transaction declined: error 402 - insufficient funds/velocity limit", requestId);
            throw new SimulatorException("Payment transaction declined");
        }

        // Normal successful payment
        long paymentLatency = TelemetryRecorder.simulateLatency(30, 80);
        telemetryRecorder.record(requestId, LogicalService.PAYMENT.name(), "PROCESS_PAYMENT",
                "/internal/payment", "POST", 200, paymentLatency);

        // Database write span (payment persisted)
        long dbLatency = TelemetryRecorder.simulateLatency(5, 20) + databaseState.getExtraQueryLatencyMs();
        telemetryRecorder.record(requestId, LogicalService.DATABASE.name(), "PERSIST_PAYMENT",
                "/internal/db/payment", "WRITE", 200, dbLatency);

        // Notification span
        long notifLatency = TelemetryRecorder.simulateLatency(15, 40);
        telemetryRecorder.record(requestId, LogicalService.NOTIFICATION.name(), "SEND_NOTIFICATION",
                "/internal/notification", "POST", 200, notifLatency);

        eventPublisher.publish(EventType.PAYMENT_PROCESSED, LogicalService.PAYMENT, requestId,
                String.format("Payment of %.2f processed for order %s", amount, orderId),
                String.format("{\"orderId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f}", orderId, userId, amount));

        eventPublisher.publish(EventType.NOTIFICATION_SENT, LogicalService.NOTIFICATION, requestId,
                "Order confirmation notification sent to user " + userId,
                String.format("{\"orderId\":\"%s\",\"userId\":\"%s\"}", orderId, userId));
    }
}
