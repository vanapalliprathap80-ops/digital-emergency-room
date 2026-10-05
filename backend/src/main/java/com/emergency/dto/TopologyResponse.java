package com.emergency.dto;

import java.util.List;

public record TopologyResponse(
        List<TopologyNode> nodes,
        List<TopologyEdge> edges
) {}
