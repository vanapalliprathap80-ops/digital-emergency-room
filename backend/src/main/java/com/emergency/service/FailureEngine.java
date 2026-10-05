package com.emergency.service;

import com.emergency.domain.*;
import com.emergency.events.EventPublisher;
import com.emergency.events.EventType;
import com.emergency.repository.IncidentTimelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FailureEngine {

    private static final Logger log = LoggerFactory.getLogger(FailureEngine.class);

    private final AtomicReference<ActiveFailureState> activeFailure = new AtomicReference<>(null);
    private final ServiceRegistry serviceRegistry;
    private final SimulatedDatabaseState databaseState;
    private final SimulatedPaymentState paymentState;
    private final SimulatedDeploymentState deploymentState;
    private final SimulatedResourceState resourceState;
    private final IncidentTimelineRepository timelineRepository;
    private final SymptomLogger symptomLogger;
    private final EventPublisher eventPublisher;
    private final Random random = new Random();

    public FailureEngine(ServiceRegistry serviceRegistry,
                         SimulatedDatabaseState databaseState,
                         SimulatedPaymentState paymentState,
                         SimulatedDeploymentState deploymentState,
                         SimulatedResourceState resourceState,
                         IncidentTimelineRepository timelineRepository,
                         SymptomLogger symptomLogger,
                         EventPublisher eventPublisher) {
        this.serviceRegistry = serviceRegistry;
        this.databaseState = databaseState;
        this.paymentState = paymentState;
        this.deploymentState = deploymentState;
        this.resourceState = resourceState;
        this.timelineRepository = timelineRepository;
        this.symptomLogger = symptomLogger;
        this.eventPublisher = eventPublisher;
    }

    public boolean hasActiveFailure() {
        return activeFailure.get() != null;
    }

    public ActiveFailureState getActiveFailure() {
        return activeFailure.get();
    }

    public void inject(Incident incident, FailureType failureType, Severity severity, TrafficPattern trafficPattern) {
        log.info("Injecting failure: type={}, severity={}, service={}",
                failureType, severity, failureType.getAffectedService());

        ActiveFailureState state = new ActiveFailureState(
                incident.getIncidentId(),
                failureType,
                failureType.getComponent(),
                severity,
                failureType.getAffectedService(),
                trafficPattern
        );
        this.activeFailure.set(state);

        // Apply physical simulator state changes according to taxonomy
        applySimulatorState(failureType, severity);

        // Record Timeline events
        addTimeline(incident.getIncidentId(), TimelineEventType.INCIDENT_INJECTED,
                failureType.getAffectedService(),
                String.format("Failure injected: %s with severity %s", failureType.name(), severity.name()),
                String.format("{\"severity\":\"%s\",\"pattern\":\"%s\"}", severity.name(), trafficPattern.name()));

        if (trafficPattern != TrafficPattern.NORMAL) {
            addTimeline(incident.getIncidentId(), TimelineEventType.TRAFFIC_CHANGED,
                    LogicalService.API_GATEWAY,
                    "Traffic pattern modified to " + trafficPattern.name() + " (" + trafficPattern.getDescription() + ")",
                    "{\"multiplier\":\" " + trafficPattern.getMultiplier() + "\"");
        }

        addTimeline(incident.getIncidentId(), TimelineEventType.SERVICE_DEGRADED,
                failureType.getAffectedService(),
                "Service " + failureType.getAffectedService().name() + " state degraded to " + serviceRegistry.getStatus(failureType.getAffectedService()).name(),
                null);

        addTimeline(incident.getIncidentId(), TimelineEventType.INCIDENT_ACTIVE,
                failureType.getAffectedService(),
                "Incident state transitioned to ACTIVE across topology",
                null);

        symptomLogger.warn(failureType.getAffectedService(),
                "Anomaly detected in " + failureType.getComponent().name() + " subsystem. Telemetry variance observed.",
                incident.getIncidentId());
    }

    private void applySimulatorState(FailureType failureType, Severity severity) {
        switch (failureType) {
            case DATABASE_UNAVAILABLE -> {
                databaseState.setAvailable(false);
                serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.UNAVAILABLE);
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
            case DATABASE_CONNECTION_POOL_EXHAUSTION -> {
                databaseState.getActiveConnections().set(databaseState.getPoolSize());
                serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.DEGRADED);
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
            case DATABASE_QUERY_LATENCY -> {
                databaseState.setExtraQueryLatencyMs(severity.getLatencyInjectionMs());
                serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.DEGRADED);
            }
            case DATABASE_QUERY_FAILURE -> {
                databaseState.setQueryFailureRate(severity.getFailureRate());
                serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.DEGRADED);
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
            case PAYMENT_TIMEOUT -> {
                paymentState.setTimeoutEnabled(true);
                paymentState.setTimeoutDurationMs(severity.getLatencyInjectionMs() + 3000);
                serviceRegistry.setState(LogicalService.PAYMENT, HealthStatus.DEGRADED);
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
            case PAYMENT_SERVICE_UNAVAILABLE -> {
                paymentState.setAvailable(false);
                serviceRegistry.setState(LogicalService.PAYMENT, HealthStatus.UNAVAILABLE);
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
            case PAYMENT_FAILURE_RATE -> {
                paymentState.setFailureRate(severity.getFailureRate());
                serviceRegistry.setState(LogicalService.PAYMENT, HealthStatus.DEGRADED);
            }
            case API_HIGH_LATENCY -> {
                serviceRegistry.setState(LogicalService.API_GATEWAY, HealthStatus.DEGRADED);
            }
            case API_5XX -> {
                serviceRegistry.setState(LogicalService.API_GATEWAY, HealthStatus.DEGRADED);
            }
            case CPU_PRESSURE -> {
                resourceState.setCpuUtilizationPercent(75.0 + (severity.getFailureRate() * 24.0));
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
            case MEMORY_PRESSURE -> {
                resourceState.setMemoryUtilizationPercent(80.0 + (severity.getFailureRate() * 19.0));
                resourceState.setGcPauseLatencyMs(severity.getLatencyInjectionMs());
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
            case BAD_DEPLOYMENT -> {
                deploymentState.setVersion("v1.1.0-rc2");
                deploymentState.setDeploymentStatus("DEGRADED");
                deploymentState.setDegradedDeployment(true);
                deploymentState.setFailureRate(severity.getFailureRate());
                serviceRegistry.setState(LogicalService.ORDERS, HealthStatus.DEGRADED);
            }
        }
    }

    public void clear() {
        log.info("Clearing all active failure states and restoring healthy baseline");
        activeFailure.set(null);
        databaseState.reset();
        paymentState.reset();
        deploymentState.reset();
        resourceState.reset();
        serviceRegistry.reset();
    }

    public void addTimeline(String incidentId, TimelineEventType type, LogicalService service, String message, String metadata) {
        IncidentTimelineEvent event = new IncidentTimelineEvent(incidentId, type, service, message, metadata);
        timelineRepository.save(event);
    }
}
