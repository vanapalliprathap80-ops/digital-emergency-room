package com.emergency.controller;

import com.emergency.dto.TelemetryResponse;
import com.emergency.repository.TelemetryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
public class RequestsController {

    private final TelemetryRepository telemetryRepository;

    public RequestsController(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    @GetMapping
    public ResponseEntity<Page<TelemetryResponse>> getRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String service) {
        if (size > 200) size = 200;

        Page<TelemetryResponse> result;
        if (service != null && !service.isBlank()) {
            result = telemetryRepository
                    .findByServiceOrderByTimestampDesc(service.toUpperCase(), PageRequest.of(page, size))
                    .map(TelemetryResponse::from);
        } else {
            result = telemetryRepository
                    .findAllByOrderByTimestampDesc(PageRequest.of(page, size))
                    .map(TelemetryResponse::from);
        }
        return ResponseEntity.ok(result);
    }
}
