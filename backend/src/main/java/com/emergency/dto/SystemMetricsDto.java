package com.emergency.dto;

import java.util.List;

public record SystemMetricsDto(
        long totalRequests,
        long totalErrors,
        double errorRate,
        double averageLatencyMs,
        long p95LatencyMs,
        double requestsPerMinute,
        List<ServiceMetricsDto> services
) {}
