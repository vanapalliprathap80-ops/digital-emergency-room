package com.emergency.controller;

import com.emergency.dto.TopologyResponse;
import com.emergency.topology.TopologyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topology")
public class TopologyController {

    private final TopologyService topologyService;

    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping
    public ResponseEntity<TopologyResponse> getTopology() {
        return ResponseEntity.ok(topologyService.getTopology());
    }
}
