package com.emergency.controller;

import com.emergency.dto.ServiceStatusDto;
import com.emergency.service.LogicalService;
import com.emergency.service.ServiceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final ServiceRegistry serviceRegistry;

    public HealthController(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        List<ServiceStatusDto> statuses = serviceRegistry.getAllStatuses();
        boolean allHealthy = statuses.stream().allMatch(s -> "HEALTHY".equals(s.status()));
        String overall = allHealthy ? "UP" : "DEGRADED";

        return ResponseEntity.ok(Map.of(
                "status", overall,
                "services", statuses
        ));
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceStatusDto>> getServices() {
        return ResponseEntity.ok(serviceRegistry.getAllStatuses());
    }

    @GetMapping("/services/{service}")
    public ResponseEntity<ServiceStatusDto> getService(@PathVariable String service) {
        LogicalService logicalService = LogicalService.valueOf(service.toUpperCase());
        return ResponseEntity.ok(serviceRegistry.getStatusDto(logicalService));
    }
}
