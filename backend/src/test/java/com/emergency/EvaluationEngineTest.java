package com.emergency;

import com.emergency.domain.*;
import com.emergency.dto.AgentDiagnosisRequest;
import com.emergency.dto.EvaluationResultResponse;
import com.emergency.repository.GroundTruthRepository;
import com.emergency.service.EvaluationEngine;
import com.emergency.service.LogicalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EvaluationEngineTest {

    @Mock
    private GroundTruthRepository groundTruthRepository;

    @InjectMocks
    private EvaluationEngine evaluationEngine;

    @Test
    void testAccuratePinpointDiagnosis() {
        String incidentId = "inc-eval-01";
        GroundTruth gt = new GroundTruth(
                incidentId,
                LogicalService.DATABASE,
                FailureComponent.CONNECTION_POOL,
                FailureType.DATABASE_CONNECTION_POOL_EXHAUSTION,
                Severity.HIGH,
                "Pool exhausted",
                TrafficPattern.NORMAL,
                100L
        );
        when(groundTruthRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(gt));

        AgentDiagnosisRequest diagnosis = new AgentDiagnosisRequest(
                incidentId,
                LogicalService.DATABASE,
                FailureComponent.CONNECTION_POOL,
                FailureType.DATABASE_CONNECTION_POOL_EXHAUSTION,
                Severity.HIGH,
                0.95
        );

        EvaluationResultResponse result = evaluationEngine.evaluate(diagnosis, "inv-eval-01");

        assertTrue(result.serviceCorrect());
        assertTrue(result.componentCorrect());
        assertTrue(result.failureTypeCorrect());
        assertTrue(result.severityCorrect());
        assertTrue(result.pinpointAccurate());
    }

    @Test
    void testVagueOrIncorrectDiagnosisFails() {
        String incidentId = "inc-eval-02";
        GroundTruth gt = new GroundTruth(
                incidentId,
                LogicalService.DATABASE,
                FailureComponent.CONNECTION_POOL,
                FailureType.DATABASE_CONNECTION_POOL_EXHAUSTION,
                Severity.HIGH,
                "Pool exhausted",
                TrafficPattern.NORMAL,
                100L
        );
        when(groundTruthRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(gt));

        // Agent mistakenly blames ORDERS service
        AgentDiagnosisRequest vagueDiagnosis = new AgentDiagnosisRequest(
                incidentId,
                LogicalService.ORDERS,
                FailureComponent.BUSINESS_LOGIC,
                FailureType.BAD_DEPLOYMENT,
                Severity.LOW,
                0.5
        );

        EvaluationResultResponse result = evaluationEngine.evaluate(vagueDiagnosis, "inv-eval-02");

        assertFalse(result.serviceCorrect());
        assertFalse(result.componentCorrect());
        assertFalse(result.failureTypeCorrect());
        assertFalse(result.pinpointAccurate());
    }
}
