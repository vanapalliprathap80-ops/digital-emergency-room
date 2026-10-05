package com.emergency.controller;

import com.emergency.dto.AgentDiagnosisRequest;
import com.emergency.dto.EvaluationResultResponse;
import com.emergency.service.EvaluationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationEngine evaluationEngine;

    public EvaluationController(EvaluationEngine evaluationEngine) {
        this.evaluationEngine = evaluationEngine;
    }

    @PostMapping("/diagnose")
    public ResponseEntity<EvaluationResultResponse> evaluateDiagnosis(@RequestBody AgentDiagnosisRequest diagnosis) {
        return ResponseEntity.ok(evaluationEngine.evaluate(diagnosis, "manual-evaluation"));
    }
}
