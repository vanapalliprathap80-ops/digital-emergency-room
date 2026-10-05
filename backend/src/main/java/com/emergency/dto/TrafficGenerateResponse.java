package com.emergency.dto;

public record TrafficGenerateResponse(
        int requested,
        int completed,
        int succeeded,
        int failed,
        long durationMs
) {}
