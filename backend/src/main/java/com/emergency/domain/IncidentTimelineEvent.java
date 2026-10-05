package com.emergency.domain;

import com.emergency.service.LogicalService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "incident_timeline")
@Getter
@Setter
@NoArgsConstructor
public class IncidentTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, length = 50)
    private String incidentId;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TimelineEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LogicalService service;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public IncidentTimelineEvent(String incidentId, TimelineEventType eventType,
                                 LogicalService service, String message, String metadata) {
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.service = service;
        this.message = message;
        this.metadata = metadata;
        this.timestamp = Instant.now();
    }
}
