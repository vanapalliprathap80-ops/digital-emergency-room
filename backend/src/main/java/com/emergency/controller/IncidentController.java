package com.emergency.controller;

import com.emergency.dto.*;
import com.emergency.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping("/manual")
    public ResponseEntity<IncidentResponse> createManualIncident(@Valid @RequestBody ManualIncidentRequest request) {
        IncidentResponse response = incidentService.createManualIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/chaos")
    public ResponseEntity<IncidentResponse> createChaosIncident(@RequestBody(required = false) ChaosIncidentRequest request) {
        ChaosIncidentRequest req = request != null ? request : new ChaosIncidentRequest(null, null, null);
        IncidentResponse response = incidentService.createChaosIncident(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> getAllIncidents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(incidentService.getAllIncidents(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineEventResponse>> getTimeline(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getTimeline(id));
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<IncidentResponse> resetIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.resetIncident(id));
    }

    @GetMapping("/system-state")
    public ResponseEntity<SimulatedSystemStateDto> getSystemState() {
        return ResponseEntity.ok(incidentService.getSimulatedSystemState());
    }
}
