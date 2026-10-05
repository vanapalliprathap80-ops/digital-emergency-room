package com.emergency.dto;

public record ServiceMetricsDto(
        String service,
        String healthStatus,
        long requestCount,
        long errorCount,
        double errorRate,
        double averageLatencyMs,
        long p95LatencyMs,
        double requestsPerMinute
) {}
