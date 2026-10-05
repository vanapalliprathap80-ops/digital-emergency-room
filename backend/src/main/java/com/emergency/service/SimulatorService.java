package com.emergency.service;

import com.emergency.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SimulatorService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);

    private final OrderRepository orderRepository;
    private final TelemetryRepository telemetryRepository;
    private final ApplicationEventRepository eventRepository;
    private final IncidentRepository incidentRepository;
    private final GroundTruthRepository groundTruthRepository;
    private final IncidentTimelineRepository timelineRepository;
    private final SimulatorLogRepository logRepository;
    private final FailureEngine failureEngine;

    public SimulatorService(OrderRepository orderRepository,
                            TelemetryRepository telemetryRepository,
                            ApplicationEventRepository eventRepository,
                            IncidentRepository incidentRepository,
                            GroundTruthRepository groundTruthRepository,
                            IncidentTimelineRepository timelineRepository,
                            SimulatorLogRepository logRepository,
                            FailureEngine failureEngine) {
        this.orderRepository = orderRepository;
        this.telemetryRepository = telemetryRepository;
        this.eventRepository = eventRepository;
        this.incidentRepository = incidentRepository;
        this.groundTruthRepository = groundTruthRepository;
        this.timelineRepository = timelineRepository;
        this.logRepository = logRepository;
        this.failureEngine = failureEngine;
    }

    @Transactional
    public Map<String, Object> reset() {
        log.info("Simulator reset initiated");

        long telemetryDeleted = telemetryRepository.count();
        long eventsDeleted = eventRepository.count();
        long ordersDeleted = orderRepository.count();
        long incidentsDeleted = incidentRepository.count();

        telemetryRepository.deleteAllTelemetry();
        eventRepository.deleteAllEvents();
        orderRepository.deleteAll();
        incidentRepository.deleteAllIncidents();
        groundTruthRepository.deleteAllGroundTruth();
        timelineRepository.deleteAllTimelineEvents();
        logRepository.deleteAllLogs();

        failureEngine.clear();

        log.info("Simulator reset complete: telemetry={}, events={}, orders={}, incidents={}",
                telemetryDeleted, eventsDeleted, ordersDeleted, incidentsDeleted);

        return Map.of(
                "status", "RESET_COMPLETE",
                "telemetryCleared", telemetryDeleted,
                "eventsCleared", eventsDeleted,
                "ordersCleared", ordersDeleted,
                "incidentsCleared", incidentsDeleted,
                "servicesRestored", "HEALTHY"
        );
    }
}
