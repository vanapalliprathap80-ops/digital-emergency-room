package com.emergency.dto;

import java.util.List;

public record TopologyNode(
        String id,
        String label,
        String type,
        String healthStatus,
        List<String> dependsOn
) {}
