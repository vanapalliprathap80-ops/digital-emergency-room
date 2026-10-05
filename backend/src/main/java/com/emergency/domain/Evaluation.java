package com.emergency.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "evaluations")
public class Evaluation {
    @Id
    private String investigationId;
    private Boolean serviceCorrect;
    private Boolean componentCorrect;
    private Boolean failureCorrect;
    private Boolean severityCorrect;

    public Evaluation() {}
    public Evaluation(String investigationId, Boolean serviceCorrect, Boolean componentCorrect, Boolean failureCorrect, Boolean severityCorrect) {
        this.investigationId = investigationId;
        this.serviceCorrect = serviceCorrect;
        this.componentCorrect = componentCorrect;
        this.failureCorrect = failureCorrect;
        this.severityCorrect = severityCorrect;
    }
    // getters and setters
    public String getInvestigationId() { return investigationId; }
    public Boolean getServiceCorrect() { return serviceCorrect; }
    public Boolean getComponentCorrect() { return componentCorrect; }
    public Boolean getFailureCorrect() { return failureCorrect; }
    public Boolean getSeverityCorrect() { return severityCorrect; }
}
