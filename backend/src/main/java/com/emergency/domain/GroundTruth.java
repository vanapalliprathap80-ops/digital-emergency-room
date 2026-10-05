package com.emergency.domain;

import com.emergency.service.LogicalService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Internal-only Ground Truth entity.
 * NEVER exposed to observable APIs, metrics, or future AI agents.
 */
@Entity
@Table(name = "ground_truth")
@Getter
@Setter
@NoArgsConstructor
public class GroundTruth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, unique = true, length = 50)
    private String incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LogicalService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private FailureComponent component;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", nullable = false, length = 100)
    private FailureType failureType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "expected_impact", columnDefinition = "TEXT")
    private String expectedImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_pattern", length = 30)
    private TrafficPattern trafficPattern;

    private Long seed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public GroundTruth(String incidentId, LogicalService service, FailureComponent component,
                       FailureType failureType, Severity severity, String expectedImpact,
                       TrafficPattern trafficPattern, Long seed) {
        this.incidentId = incidentId;
        this.service = service;
        this.component = component;
        this.failureType = failureType;
        this.severity = severity;
        this.expectedImpact = expectedImpact;
        this.trafficPattern = trafficPattern;
        this.seed = seed;
        this.createdAt = Instant.now();
    }
}
