package com.emergency.dto;

import com.emergency.domain.SimulatorLog;
import com.emergency.service.LogicalService;

import java.time.Instant;

public record SimulatorLogResponse(
        Long id,
        Instant timestamp,
        LogicalService service,
        String level,
        String message,
        String requestId,
        String metadata
) {
    public static SimulatorLogResponse from(SimulatorLog l) {
        return new SimulatorLogResponse(
                l.getId(),
                l.getTimestamp(),
                l.getService(),
                l.getLevel(),
                l.getMessage(),
                l.getRequestId(),
                l.getMetadata()
        );
    }
}
