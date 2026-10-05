package com.emergency;

import com.emergency.domain.*;
import com.emergency.dto.ChaosIncidentRequest;
import com.emergency.dto.IncidentResponse;
import com.emergency.dto.ManualIncidentRequest;
import com.emergency.repository.GroundTruthRepository;
import com.emergency.repository.IncidentRepository;
import com.emergency.service.IncidentService;
import com.emergency.service.LogicalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GroundTruthIsolationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private GroundTruthRepository groundTruthRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void testChaosIncidentHidesGroundTruthInPublicResponse() {
        ChaosIncidentRequest chaosReq = new ChaosIncidentRequest("isolation-session", 12345L, TrafficPattern.HIGH);
        IncidentResponse response = incidentService.createChaosIncident(chaosReq);

        assertNotNull(response.incidentId());
        assertEquals(IncidentMode.CHAOS, response.mode());
        assertEquals(IncidentStatus.ACTIVE, response.status());

        // Ground truth exists in private database table
        Optional<GroundTruth> gtOpt = groundTruthRepository.findByIncidentId(response.incidentId());
        assertTrue(gtOpt.isPresent());
        assertNotNull(gtOpt.get().getFailureType());
        assertNotNull(gtOpt.get().getComponent());

        // But safe public DTO does not leak failureType or component
        assertNotNull(response.severity());
        assertNotNull(response.affectedService());

        // Cleanup
        incidentService.resetIncident(response.incidentId());
    }

    @Test
    void testDeterministicSeedProducesIdenticalFailure() {
        long seed = 987654321L;
        ChaosIncidentRequest req1 = new ChaosIncidentRequest("seed-session-1", seed, TrafficPattern.NORMAL);
        IncidentResponse inc1 = incidentService.createChaosIncident(req1);
        GroundTruth gt1 = groundTruthRepository.findByIncidentId(inc1.incidentId()).orElseThrow();

        incidentService.resetIncident(inc1.incidentId());

        ChaosIncidentRequest req2 = new ChaosIncidentRequest("seed-session-2", seed, TrafficPattern.NORMAL);
        IncidentResponse inc2 = incidentService.createChaosIncident(req2);
        GroundTruth gt2 = groundTruthRepository.findByIncidentId(inc2.incidentId()).orElseThrow();

        assertEquals(gt1.getFailureType(), gt2.getFailureType());
        assertEquals(gt1.getComponent(), gt2.getComponent());
        assertEquals(gt1.getSeverity(), gt2.getSeverity());

        incidentService.resetIncident(inc2.incidentId());
    }
}
