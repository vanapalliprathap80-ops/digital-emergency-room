package com.emergency.controller;

import com.emergency.domain.TelemetryRecord;
import com.emergency.repository.TelemetryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryIngestionController {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionController.class);
    
    private final TelemetryRepository telemetryRepository;

    public TelemetryIngestionController(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@RequestBody TelemetryRecord record) {
        try {
            telemetryRepository.save(record);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Failed to ingest telemetry: {}", ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
