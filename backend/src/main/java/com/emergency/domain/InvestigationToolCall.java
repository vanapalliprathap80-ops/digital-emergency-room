package com.emergency.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investigation_tool_calls")
public class InvestigationToolCall {
    @Id
    private String id;
    private String investigationId;
    private String toolName;
    private String inputArgs;
    private String outputSummary;
    private boolean success;
    private long durationMs;
    private Instant timestamp;

    public InvestigationToolCall() {}
    public InvestigationToolCall(String investigationId, String toolName, String inputArgs, String outputSummary, boolean success, long durationMs) {
        this.id = UUID.randomUUID().toString();
        this.investigationId = investigationId;
        this.toolName = toolName;
        this.inputArgs = inputArgs;
        this.outputSummary = outputSummary;
        this.success = success;
        this.durationMs = durationMs;
        this.timestamp = Instant.now();
    }
    // getters and setters
    public String getId() { return id; }
    public String getInvestigationId() { return investigationId; }
    public String getToolName() { return toolName; }
    public String getInputArgs() { return inputArgs; }
    public String getOutputSummary() { return outputSummary; }
    public boolean isSuccess() { return success; }
    public long getDurationMs() { return durationMs; }
    public Instant getTimestamp() { return timestamp; }
}
