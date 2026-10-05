package com.emergency.service;

import com.emergency.telemetry.TelemetryRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logical AUTH service.
 * Validates that the request has an authenticated user.
 * Phase 1: all users are considered authenticated (lightweight simulation).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final TelemetryRecorder telemetryRecorder;
    private final ServiceRegistry serviceRegistry;

    public AuthService(TelemetryRecorder telemetryRecorder, ServiceRegistry serviceRegistry) {
        this.telemetryRecorder = telemetryRecorder;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Simulate auth validation.
     * Returns the userId if authenticated, throws if not.
     */
    public String authenticate(String requestId, String userId) {
        long start = System.currentTimeMillis();
        long latency = TelemetryRecorder.simulateLatency(10, 30);

        log.debug("Auth check for userId={} requestId={}", userId, requestId);

        // Phase 1: always authenticates
        telemetryRecorder.record(requestId, LogicalService.AUTH.name(), "AUTHENTICATE",
                "/internal/auth", "POST", 200, latency);

        return userId;
    }
}
