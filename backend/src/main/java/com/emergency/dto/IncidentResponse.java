package com.emergency.dto;

import com.emergency.domain.Incident;
import com.emergency.domain.IncidentMode;
import com.emergency.domain.IncidentStatus;
import com.emergency.domain.Severity;
import com.emergency.domain.TrafficPattern;
import com.emergency.service.LogicalService;

import java.time.Instant;

/**
 * Safe Incident DTO.
 * Notice: For Chaos Mode, ground-truth failureType is omitted to prevent leakage.
 */
public record IncidentResponse(
        String incidentId,
        String sessionId,
        IncidentStatus status,
        IncidentMode mode,
        Severity severity,
        LogicalService affectedService,
        TrafficPattern trafficPattern,
        Instant createdAt,
        Instant startedAt,
        Instant endedAt
) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(
                i.getIncidentId(),
                i.getSessionId(),
                i.getStatus(),
                i.getMode(),
                i.getSeverity(),
                // Hide affectedService in CHAOS mode if active/recovering so agent has to find it out
                (i.getMode() == IncidentMode.CHAOS && (i.getStatus() == IncidentStatus.ACTIVE || i.getStatus() == IncidentStatus.RECOVERING)) ? null : i.getAffectedService(),
                i.getTrafficPattern(),
                i.getCreatedAt(),
                i.getStartedAt(),
                i.getEndedAt()
        );
    }
}
