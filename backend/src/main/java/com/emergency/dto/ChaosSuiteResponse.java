package com.emergency.dto;

import java.util.List;

public record ChaosSuiteResponse(
        int totalRequested,
        int incidentsInjected,
        int incidentsCompleted,
        int injectionFailures,
        double resetSuccessRate,
        long telemetryObserved,
        long expectedDegradationObserved,
        List<String> incidentSummaries
) {}
