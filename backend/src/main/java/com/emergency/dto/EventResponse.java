package com.emergency.dto;

import com.emergency.domain.ApplicationEvent;

import java.time.Instant;

public record EventResponse(
        Long id,
        String eventId,
        Instant timestamp,
        String requestId,
        String eventType,
        String service,
        String message,
        String metadata
) {
    public static EventResponse from(ApplicationEvent e) {
        return new EventResponse(
                e.getId(), e.getEventId(), e.getTimestamp(),
                e.getRequestId(), e.getEventType(), e.getService(),
                e.getMessage(), e.getMetadata()
        );
    }
}
