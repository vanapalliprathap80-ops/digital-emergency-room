package com.emergency.dto;

import com.emergency.domain.IncidentTimelineEvent;
import com.emergency.domain.TimelineEventType;
import com.emergency.service.LogicalService;

import java.time.Instant;

public record TimelineEventResponse(
        Long id,
        String incidentId,
        Instant timestamp,
        TimelineEventType eventType,
        LogicalService service,
        String message,
        String metadata
) {
    public static TimelineEventResponse from(IncidentTimelineEvent e) {
        return new TimelineEventResponse(
                e.getId(),
                e.getIncidentId(),
                e.getTimestamp(),
                e.getEventType(),
                e.getService(),
                e.getMessage(),
                e.getMetadata()
        );
    }
}
