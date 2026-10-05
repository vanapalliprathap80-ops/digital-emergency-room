package com.emergency.controller;

import com.emergency.domain.RemediationAction;
import com.emergency.remediation.RemediationService;
import com.emergency.repository.RemediationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/remediations")
@RequiredArgsConstructor
public class RemediationController {

    private final RemediationService remediationService;
    private final RemediationRepository remediationRepository;

    @GetMapping("/incident/{incidentId}")
    public List<RemediationAction> getActionsForIncident(@PathVariable String incidentId) {
        return remediationRepository.findByIncidentIdOrderByProposedAtDesc(incidentId);
    }

    @GetMapping("/{actionId}")
    public ResponseEntity<RemediationAction> getAction(@PathVariable String actionId) {
        return remediationRepository.findById(actionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{actionId}/approve")
    public RemediationAction approveAction(@PathVariable String actionId, @RequestBody(required = false) Map<String, String> body) {
        String approvedBy = (body != null && body.containsKey("approvedBy")) ? body.get("approvedBy") : "Human Operator";
        return remediationService.approveAction(actionId, approvedBy);
    }

    @PostMapping("/{actionId}/reject")
    public RemediationAction rejectAction(@PathVariable String actionId, @RequestBody(required = false) Map<String, String> body) {
        String rejectedBy = (body != null && body.containsKey("rejectedBy")) ? body.get("rejectedBy") : "Human Operator";
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "No reason provided";
        return remediationService.rejectAction(actionId, rejectedBy, reason);
    }
}
