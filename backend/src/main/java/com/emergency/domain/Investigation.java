package com.emergency.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investigations")
public class Investigation {
    @Id
    private String id;
    private String incidentId;
    private String engine;
    @Enumerated(EnumType.STRING)
    private InvestigationStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private int toolCallCount;
    @Enumerated(EnumType.STRING)
    private DiagnosisStatus diagnosisStatus;
    private String rootCauseService;
    private String rootCauseComponent;
    private String rootCauseFailure;
    private String confidence;
    private String impact;
    private String recommendedAction;

    public Investigation() {}
    public Investigation(String incidentId, String engine) {
        this.id = UUID.randomUUID().toString();
        this.incidentId = incidentId;
        this.engine = engine;
        this.status = InvestigationStatus.RUNNING;
        this.startedAt = Instant.now();
        this.toolCallCount = 0;
    }
    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }
    public InvestigationStatus getStatus() { return status; }
    public void setStatus(InvestigationStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public int getToolCallCount() { return toolCallCount; }
    public void setToolCallCount(int toolCallCount) { this.toolCallCount = toolCallCount; }
    public void incrementToolCallCount() { this.toolCallCount++; }
    public DiagnosisStatus getDiagnosisStatus() { return diagnosisStatus; }
    public void setDiagnosisStatus(DiagnosisStatus diagnosisStatus) { this.diagnosisStatus = diagnosisStatus; }
    public String getRootCauseService() { return rootCauseService; }
    public void setRootCauseService(String rootCauseService) { this.rootCauseService = rootCauseService; }
    public String getRootCauseComponent() { return rootCauseComponent; }
    public void setRootCauseComponent(String rootCauseComponent) { this.rootCauseComponent = rootCauseComponent; }
    public String getRootCauseFailure() { return rootCauseFailure; }
    public void setRootCauseFailure(String rootCauseFailure) { this.rootCauseFailure = rootCauseFailure; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
}
