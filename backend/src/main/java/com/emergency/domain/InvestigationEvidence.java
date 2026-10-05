package com.emergency.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "investigation_evidence")
public class InvestigationEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String investigationId;
    private String source;
    private String observation;
    private Instant createdAt;

    public InvestigationEvidence() {}
    public InvestigationEvidence(String investigationId, String source, String observation) {
        this.investigationId = investigationId;
        this.source = source;
        this.observation = observation;
        this.createdAt = Instant.now();
    }
    // getters and setters
    public Long getId() { return id; }
    public String getInvestigationId() { return investigationId; }
    public String getSource() { return source; }
    public String getObservation() { return observation; }
    public Instant getCreatedAt() { return createdAt; }
}
