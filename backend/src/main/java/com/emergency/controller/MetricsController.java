package com.emergency.controller;

import com.emergency.dto.ServiceMetricsDto;
import com.emergency.dto.SystemMetricsDto;
import com.emergency.metrics.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<SystemMetricsDto> getSystemMetrics() {
        return ResponseEntity.ok(metricsService.getSystemMetrics());
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceMetricsDto>> getServiceMetrics() {
        return ResponseEntity.ok(metricsService.getAllServiceMetrics());
    }
}
