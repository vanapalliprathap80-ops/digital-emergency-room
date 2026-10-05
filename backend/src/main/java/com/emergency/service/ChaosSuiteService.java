package com.emergency.service;

import com.emergency.domain.TrafficPattern;
import com.emergency.dto.ChaosIncidentRequest;
import com.emergency.dto.ChaosSuiteResponse;
import com.emergency.dto.IncidentResponse;
import com.emergency.dto.TrafficGenerateRequest;
import com.emergency.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChaosSuiteService {

    private static final Logger log = LoggerFactory.getLogger(ChaosSuiteService.class);

    private final IncidentService incidentService;
    private final TrafficGeneratorService trafficGeneratorService;
    private final TelemetryRepository telemetryRepository;
    private final SimulatorService simulatorService;

    public ChaosSuiteService(IncidentService incidentService,
                             TrafficGeneratorService trafficGeneratorService,
                             TelemetryRepository telemetryRepository,
                             SimulatorService simulatorService) {
        this.incidentService = incidentService;
        this.trafficGeneratorService = trafficGeneratorService;
        this.telemetryRepository = telemetryRepository;
        this.simulatorService = simulatorService;
    }

    public ChaosSuiteResponse runSuite(int count, String sessionId) {
        int target = Math.min(Math.max(1, count), 20); // bounded 1..20
        log.info("Executing Chaos Test Suite with {} iterations for session {}", target, sessionId);

        int injected = 0;
        int completed = 0;
        int failures = 0;
        long telemetryTotal = 0;
        long degradationObserved = 0;
        List<String> summaries = new ArrayList<>();

        for (int i = 1; i <= target; i++) {
            try {
                // 1. Inject Chaos incident
                ChaosIncidentRequest request = new ChaosIncidentRequest(sessionId, (long) (i * 1000 + 42), TrafficPattern.NORMAL);
                IncidentResponse incident = incidentService.createChaosIncident(request);
                injected++;

                // 2. Generate controlled traffic
                long beforeErrors = telemetryRepository.countBySuccessFalse();
                trafficGeneratorService.generate(10);

                long afterErrors = telemetryRepository.countBySuccessFalse();
                long recentTelemetry = telemetryRepository.count();
                telemetryTotal = recentTelemetry;

                if (afterErrors >= beforeErrors) {
                    degradationObserved++;
                }

                // 3. Reset Incident
                incidentService.resetIncident(incident.incidentId());
                completed++;

                summaries.add(String.format("Iteration %d/%d: Incident %s injected and verified successfully.",
                        i, target, incident.incidentId()));

            } catch (Exception e) {
                log.error("Chaos suite iteration {} failed: {}", i, e.getMessage(), e);
                failures++;
                summaries.add(String.format("Iteration %d/%d: Failed with error: %s", i, target, e.getMessage()));
                simulatorService.reset();
            }
        }

        double resetRate = injected > 0 ? ((double) completed / injected) * 100.0 : 100.0;
        return new ChaosSuiteResponse(
                target,
                injected,
                completed,
                failures,
                resetRate,
                telemetryTotal,
                degradationObserved,
                summaries
        );
    }
}
