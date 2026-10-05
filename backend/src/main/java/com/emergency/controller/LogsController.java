package com.emergency.controller;

import com.emergency.domain.SimulatorLog;
import com.emergency.dto.SimulatorLogResponse;
import com.emergency.repository.SimulatorLogRepository;
import com.emergency.service.LogicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogsController {

    private final SimulatorLogRepository logRepository;

    public LogsController(SimulatorLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping
    public ResponseEntity<Page<SimulatorLogResponse>> getLogs(
            @RequestParam(required = false) LogicalService service,
            @RequestParam(required = false) String level,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<SimulatorLog> page;
        if (service != null) {
            page = logRepository.findByServiceOrderByTimestampDesc(service, pageable);
        } else if (level != null) {
            page = logRepository.findByLevelOrderByTimestampDesc(level, pageable);
        } else {
            page = logRepository.findAllByOrderByTimestampDesc(pageable);
        }

        return ResponseEntity.ok(page.map(SimulatorLogResponse::from));
    }
}
