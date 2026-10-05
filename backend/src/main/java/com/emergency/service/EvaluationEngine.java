package com.emergency.service;

import com.emergency.domain.Evaluation;
import com.emergency.domain.GroundTruth;
import com.emergency.dto.AgentDiagnosisRequest;
import com.emergency.dto.EvaluationResultResponse;
import com.emergency.repository.EvaluationRepository;
import com.emergency.repository.GroundTruthRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Evaluation Engine Foundation.
 * Compares hidden internal Ground Truth against future AI Agent Diagnosis.
 */
@Service
public class EvaluationEngine {

    private final GroundTruthRepository groundTruthRepository;
    private final EvaluationRepository evaluationRepository;

    public EvaluationEngine(GroundTruthRepository groundTruthRepository, EvaluationRepository evaluationRepository) {
        this.groundTruthRepository = groundTruthRepository;
        this.evaluationRepository = evaluationRepository;
    }

    public EvaluationResultResponse evaluate(AgentDiagnosisRequest diagnosis, String investigationId) {
        Optional<GroundTruth> opt = groundTruthRepository.findByIncidentId(diagnosis.incidentId());
        if (opt.isEmpty()) {
            return new EvaluationResultResponse(diagnosis.incidentId(), false, false, false, false, false);
        }

        GroundTruth gt = opt.get();
        boolean serviceMatch = gt.getService() == diagnosis.service();
        boolean componentMatch = gt.getComponent() == diagnosis.component();
        boolean failureTypeMatch = gt.getFailureType() == diagnosis.failureType();
        boolean severityMatch = gt.getSeverity() == diagnosis.severity();
        boolean pinpointAccurate = serviceMatch && componentMatch && failureTypeMatch;
        
        Evaluation evaluation = new Evaluation(investigationId, serviceMatch, componentMatch, failureTypeMatch, severityMatch);
        evaluationRepository.save(evaluation);

        return new EvaluationResultResponse(
                diagnosis.incidentId(),
                serviceMatch,
                componentMatch,
                failureTypeMatch,
                severityMatch,
                pinpointAccurate
        );
    }
}
