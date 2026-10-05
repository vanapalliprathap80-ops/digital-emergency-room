package com.emergency.controller;

import com.emergency.domain.Investigation;
import com.emergency.domain.InvestigationEvidence;
import com.emergency.domain.InvestigationToolCall;
import com.emergency.investigation.AgentOrchestrator;
import com.emergency.repository.InvestigationEvidenceRepository;
import com.emergency.repository.InvestigationRepository;
import com.emergency.repository.InvestigationToolCallRepository;
import com.emergency.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/investigations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvestigationController {

    private final AgentOrchestrator orchestrator;
    private final InvestigationRepository investigationRepo;
    private final InvestigationEvidenceRepository evidenceRepo;
    private final InvestigationToolCallRepository toolCallRepo;
    private final com.emergency.repository.EvaluationRepository evaluationRepo;

    @PostMapping("/{incidentId}/start")
    public ResponseEntity<?> startInvestigation(@PathVariable String incidentId) {
        Optional<Investigation> existing = investigationRepo.findByIncidentId(incidentId);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Investigation already exists for this incident"));
        }
        
        try {
            Investigation investigation = orchestrator.startInvestigation(incidentId);
            return ResponseEntity.ok(investigation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<?> getInvestigation(@PathVariable String incidentId) {
        Optional<Investigation> investigationOpt = investigationRepo.findByIncidentId(incidentId);
        if (investigationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Investigation inv = investigationOpt.get();
        List<InvestigationEvidence> evidence = evidenceRepo.findByInvestigationIdOrderByCreatedAtAsc(inv.getId());
        List<InvestigationToolCall> toolCalls = toolCallRepo.findByInvestigationIdOrderByTimestampAsc(inv.getId());
        
        Optional<com.emergency.domain.Evaluation> evaluation = evaluationRepo.findById(inv.getId());
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("investigation", inv);
        response.put("evidence", evidence);
        response.put("toolCalls", toolCalls);
        evaluation.ifPresent(e -> response.put("evaluation", e));
        
        return ResponseEntity.ok(response);
    }
}
