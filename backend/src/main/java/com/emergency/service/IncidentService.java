package com.emergency.service;

import com.emergency.domain.*;
import com.emergency.dto.*;
import com.emergency.exception.SimulatorException;
import com.emergency.repository.GroundTruthRepository;
import com.emergency.repository.IncidentRepository;
import com.emergency.repository.IncidentTimelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final GroundTruthRepository groundTruthRepository;
    private final IncidentTimelineRepository timelineRepository;
    private final IncidentStateMachine stateMachine;
    private final FailureEngine failureEngine;
    private final SimulatedDatabaseState databaseState;
    private final SimulatedResourceState resourceState;
    private final SimulatedDeploymentState deploymentState;
    private final SimulatedPaymentState paymentState;

    public IncidentService(IncidentRepository incidentRepository,
                           GroundTruthRepository groundTruthRepository,
                           IncidentTimelineRepository timelineRepository,
                           IncidentStateMachine stateMachine,
                           FailureEngine failureEngine,
                           SimulatedDatabaseState databaseState,
                           SimulatedResourceState resourceState,
                           SimulatedDeploymentState deploymentState,
                           SimulatedPaymentState paymentState) {
        this.incidentRepository = incidentRepository;
        this.groundTruthRepository = groundTruthRepository;
        this.timelineRepository = timelineRepository;
        this.stateMachine = stateMachine;
        this.failureEngine = failureEngine;
        this.databaseState = databaseState;
        this.resourceState = resourceState;
        this.deploymentState = deploymentState;
        this.paymentState = paymentState;
    }

    @Transactional
    public IncidentResponse createManualIncident(ManualIncidentRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : "default-session";
        ensureNoActiveIncident(sessionId);

        String incidentId = "inc-" + UUID.randomUUID().toString().substring(0, 8);
        TrafficPattern pattern = request.trafficPattern() != null ? request.trafficPattern() : TrafficPattern.NORMAL;

        Incident incident = new Incident(
                incidentId,
                sessionId,
                IncidentMode.MANUAL,
                request.severity(),
                request.service(),
                pattern,
                null
        );

        // Save internal ground truth
        GroundTruth groundTruth = new GroundTruth(
                incidentId,
                request.service(),
                request.failureType().getComponent(),
                request.failureType(),
                request.severity(),
                request.failureType().getDescription(),
                pattern,
                null
        );
        groundTruthRepository.save(groundTruth);

        // State Machine transition: CREATED -> INJECTED -> ACTIVE
        stateMachine.transition(incident, IncidentStatus.INJECTED);
        failureEngine.addTimeline(incidentId, TimelineEventType.INCIDENT_CREATED, request.service(),
                "Manual incident " + incidentId + " registered in session " + sessionId, null);

        stateMachine.transition(incident, IncidentStatus.ACTIVE);
        incidentRepository.save(incident);

        // Inject into failure engine
        failureEngine.inject(incident, request.failureType(), request.severity(), pattern);

        log.info("Manual incident {} active in session {}", incidentId, sessionId);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse createChaosIncident(ChaosIncidentRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : "default-session";
        ensureNoActiveIncident(sessionId);

        long seed = request.seed() != null ? request.seed() : System.currentTimeMillis();
        Random rng = new Random(seed);

        FailureType[] allFailures = FailureType.values();
        FailureType chosenFailure = allFailures[rng.nextInt(allFailures.length)];

        Severity[] severities = Severity.values();
        Severity chosenSeverity = severities[rng.nextInt(severities.length)];

        TrafficPattern pattern = request.trafficPattern() != null ? request.trafficPattern() :
                TrafficPattern.values()[rng.nextInt(TrafficPattern.values().length)];

        String incidentId = "chaos-" + UUID.randomUUID().toString().substring(0, 8);

        Incident incident = new Incident(
                incidentId,
                sessionId,
                IncidentMode.CHAOS,
                chosenSeverity,
                chosenFailure.getAffectedService(),
                pattern,
                seed
        );

        // Save Ground Truth internally
        GroundTruth groundTruth = new GroundTruth(
                incidentId,
                chosenFailure.getAffectedService(),
                chosenFailure.getComponent(),
                chosenFailure,
                chosenSeverity,
                chosenFailure.getDescription(),
                pattern,
                seed
        );
        groundTruthRepository.save(groundTruth);

        stateMachine.transition(incident, IncidentStatus.INJECTED);
        failureEngine.addTimeline(incidentId, TimelineEventType.INCIDENT_CREATED, chosenFailure.getAffectedService(),
                "Chaos incident " + incidentId + " created with random seed",
                "{\"mode\":\"CHAOS\"}");

        stateMachine.transition(incident, IncidentStatus.ACTIVE);
        incidentRepository.save(incident);

        failureEngine.inject(incident, chosenFailure, chosenSeverity, pattern);

        log.info("Chaos incident {} created and activated with seed {}", incidentId, seed);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse resetIncident(String incidentId) {
        Incident incident = incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new SimulatorException("Incident not found: " + incidentId));

        if (incident.getStatus() != IncidentStatus.RESET && incident.getStatus() != IncidentStatus.RESOLVED) {
            stateMachine.transition(incident, IncidentStatus.RESET);
            incidentRepository.save(incident);
        }

        failureEngine.addTimeline(incidentId, TimelineEventType.INCIDENT_RESET, null,
                "Incident " + incidentId + " manually reset. Deactivating all failure injections.", null);

        failureEngine.clear();
        log.info("Incident {} reset", incidentId);
        return IncidentResponse.from(incident);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
                .map(IncidentResponse::from)
                .orElseThrow(() -> new SimulatorException("Incident not found: " + incidentId));
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(Pageable pageable) {
        return incidentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(IncidentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimeline(String incidentId) {
        return timelineRepository.findByIncidentIdOrderByTimestampAsc(incidentId)
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SimulatedSystemStateDto getSimulatedSystemState() {
        ActiveFailureState active = failureEngine.getActiveFailure();
        return new SimulatedSystemStateDto(
                databaseState.getPoolSize(),
                databaseState.getActiveConnections().get(),
                databaseState.getAvailableConnections(),
                databaseState.getAcquisitionFailures().get(),
                databaseState.isAvailable(),
                resourceState.getCpuUtilizationPercent(),
                resourceState.getMemoryUtilizationPercent(),
                deploymentState.getVersion(),
                deploymentState.getDeploymentStatus(),
                paymentState.isAvailable(),
                active != null,
                active != null ? active.getIncidentId() : null
        );
    }

    /**
     * Automatic cleanup for abandoned incidents (e.g. no activity for > 15 minutes).
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupAbandonedIncidents() {
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Incident> abandoned = incidentRepository.findAbandonedIncidents(cutoff);
        for (Incident incident : abandoned) {
            log.warn("Cleaning up abandoned incident: {}", incident.getIncidentId());
            try {
                stateMachine.transition(incident, IncidentStatus.RESOLVED);
                incidentRepository.save(incident);
                failureEngine.addTimeline(incident.getIncidentId(), TimelineEventType.INCIDENT_RESOLVED, null,
                        "Incident automatically resolved due to inactivity timeout.", null);
                failureEngine.clear();
            } catch (Exception e) {
                log.error("Failed to resolve abandoned incident: {}", incident.getIncidentId(), e);
            }
        }
    }

    private void ensureNoActiveIncident(String sessionId) {
        incidentRepository.findBySessionIdAndStatusIn(sessionId, List.of(IncidentStatus.ACTIVE, IncidentStatus.INJECTED))
                .ifPresent(existing -> {
                    throw new SimulatorException(String.format(
                            "Another incident '%s' is currently active in session '%s'. Reset it before injecting a new one.",
                            existing.getIncidentId(), sessionId));
                });
    }
}
