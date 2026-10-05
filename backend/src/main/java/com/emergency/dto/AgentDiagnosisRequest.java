package com.emergency.dto;

import com.emergency.domain.FailureComponent;
import com.emergency.domain.FailureType;
import com.emergency.domain.Severity;
import com.emergency.service.LogicalService;

public record AgentDiagnosisRequest(
        String incidentId,
        LogicalService service,
        FailureComponent component,
        FailureType failureType,
        Severity severity,
        Double confidence
) {}
