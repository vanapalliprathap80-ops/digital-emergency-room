package com.emergency.controller;

import com.emergency.dto.TrafficGenerateRequest;
import com.emergency.dto.TrafficGenerateResponse;
import com.emergency.service.TrafficGeneratorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private final TrafficGeneratorService trafficGeneratorService;

    public TrafficController(TrafficGeneratorService trafficGeneratorService) {
        this.trafficGeneratorService = trafficGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<TrafficGenerateResponse> generate(
            @Valid @RequestBody TrafficGenerateRequest request) {
        TrafficGenerateResponse response = trafficGeneratorService.generate(request.count());
        return ResponseEntity.ok(response);
    }
}
