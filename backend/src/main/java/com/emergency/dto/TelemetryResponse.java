package com.emergency.dto;

import com.emergency.domain.TelemetryRecord;

import java.time.Instant;

public record TelemetryResponse(
        Long id,
        String requestId,
        Instant timestamp,
        String service,
        String operation,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        boolean success,
        long latencyMs,
        String errorType,
        String message
) {
    public static TelemetryResponse from(TelemetryRecord r) {
        return new TelemetryResponse(
                r.getId(), r.getRequestId(), r.getTimestamp(),
                r.getService(), r.getOperation(), r.getEndpoint(),
                r.getHttpMethod(), r.getStatusCode(), r.isSuccess(),
                r.getLatencyMs(), r.getErrorType(), r.getMessage()
        );
    }
}
