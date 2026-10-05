package com.emergency.telemetry;

import com.emergency.domain.TelemetryRecord;
import com.emergency.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TelemetryRecorder {

    private static final Logger log = LoggerFactory.getLogger(TelemetryRecorder.class);

    private final TelemetryRepository telemetryRepository;

    public TelemetryRecorder(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    public static String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static long simulateLatency(long baseMs, long jitterMs) {
        return baseMs + ThreadLocalRandom.current().nextLong(0, jitterMs + 1);
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public TelemetryRecord record(String requestId, String service, String operation,
                                  String endpoint, String httpMethod, int statusCode,
                                  long latencyMs) {
        return persist(new TelemetryRecord(requestId, service, operation, endpoint,
                httpMethod, statusCode, statusCode < 400, latencyMs, null,
                operation + " completed"));
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public TelemetryRecord record(String requestId, String service, String operation,
                                  String endpoint, String httpMethod, int statusCode,
                                  long latencyMs, String errorType, String message) {
        return persist(new TelemetryRecord(requestId, service, operation, endpoint,
                httpMethod, statusCode, statusCode < 400, latencyMs, errorType, message));
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public TelemetryRecord recordError(String requestId, String service, String operation,
                                       String endpoint, String httpMethod, int statusCode,
                                       long latencyMs, String errorType, String message) {
        return persist(new TelemetryRecord(requestId, service, operation, endpoint,
                httpMethod, statusCode, false, latencyMs, errorType, message));
    }

    private TelemetryRecord persist(TelemetryRecord record) {
        try {
            return telemetryRepository.save(record);
        } catch (Exception ex) {
            log.error("Failed to persist telemetry for request {}: {}", record.getRequestId(), ex.getMessage());
            return record;
        }
    }
}
