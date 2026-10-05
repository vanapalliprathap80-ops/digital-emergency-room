package com.emergency.dto;

import com.emergency.domain.TrafficPattern;

public record ChaosIncidentRequest(
        String sessionId,
        Long seed,
        TrafficPattern trafficPattern
) {}
