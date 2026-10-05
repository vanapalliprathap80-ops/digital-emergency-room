package com.emergency.controller;

import com.emergency.dto.ChaosSuiteResponse;
import com.emergency.service.ChaosSuiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chaos")
public class ChaosSuiteController {

    private final ChaosSuiteService chaosSuiteService;

    public ChaosSuiteController(ChaosSuiteService chaosSuiteService) {
        this.chaosSuiteService = chaosSuiteService;
    }

    @PostMapping("/suite")
    public ResponseEntity<ChaosSuiteResponse> runSuite(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "default-session") String sessionId) {
        return ResponseEntity.ok(chaosSuiteService.runSuite(count, sessionId));
    }
}
