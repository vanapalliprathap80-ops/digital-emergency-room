package com.emergency.domain;

import com.emergency.service.LogicalService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false, unique = true, length = 50)
    private String incidentId;

    @Column(name = "session_id", nullable = false, length = 50)
    private String sessionId = "default-session";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentMode mode = IncidentMode.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity = Severity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "affected_service", nullable = false, length = 50)
    private LogicalService affectedService;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_pattern", nullable = false, length = 30)
    private TrafficPattern trafficPattern = TrafficPattern.NORMAL;

    private Long seed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public Incident(String incidentId, String sessionId, IncidentMode mode,
                    Severity severity, LogicalService affectedService,
                    TrafficPattern trafficPattern, Long seed) {
        this.incidentId = incidentId;
        this.sessionId = sessionId != null ? sessionId : "default-session";
        this.mode = mode;
        this.severity = severity;
        this.affectedService = affectedService;
        this.trafficPattern = trafficPattern != null ? trafficPattern : TrafficPattern.NORMAL;
        this.seed = seed;
        this.status = IncidentStatus.CREATED;
        this.createdAt = Instant.now();
    }
}
