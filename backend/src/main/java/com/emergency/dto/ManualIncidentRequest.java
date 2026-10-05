package com.emergency.dto;

import com.emergency.domain.FailureType;
import com.emergency.domain.Severity;
import com.emergency.domain.TrafficPattern;
import com.emergency.service.LogicalService;
import jakarta.validation.constraints.NotNull;

public record ManualIncidentRequest(
        @NotNull(message = "service is required")
        LogicalService service,

        @NotNull(message = "failureType is required")
        FailureType failureType,

        @NotNull(message = "severity is required")
        Severity severity,

        TrafficPattern trafficPattern,
        String sessionId
) {}
