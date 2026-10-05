package com.emergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "application_events")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 50)
    private String eventId;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String service;

    @Column(columnDefinition = "TEXT")
    private String message;

    // JSON-encoded key/value pairs for Phase 3 correlation
    @Column(columnDefinition = "TEXT")
    private String metadata;

    public ApplicationEvent(String eventId, String requestId, String eventType,
                            String service, String message, String metadata) {
        this.eventId = eventId;
        this.requestId = requestId;
        this.eventType = eventType;
        this.service = service;
        this.message = message;
        this.metadata = metadata;
        this.timestamp = Instant.now();
    }
}
