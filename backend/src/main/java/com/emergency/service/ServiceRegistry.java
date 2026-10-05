package com.emergency.service;

import com.emergency.dto.ServiceStatusDto;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of logical service health states.
 * Health is now dynamically derived from the underlying physical simulated states.
 */
@Component
public class ServiceRegistry {

    private final Map<LogicalService, HealthStatus> manualStates = new ConcurrentHashMap<>();
    private final SimulatedDatabaseState databaseState;
    private final SimulatedPaymentState paymentState;
    private final SimulatedDeploymentState deploymentState;
    private final SimulatedResourceState resourceState;

    public ServiceRegistry(SimulatedDatabaseState databaseState,
                           SimulatedPaymentState paymentState,
                           SimulatedDeploymentState deploymentState,
                           SimulatedResourceState resourceState) {
        this.databaseState = databaseState;
        this.paymentState = paymentState;
        this.deploymentState = deploymentState;
        this.resourceState = resourceState;
        reset();
    }

    public HealthStatus getStatus(LogicalService service) {
        if (service == LogicalService.DATABASE) {
            if (!databaseState.isAvailable()) return HealthStatus.UNAVAILABLE;
            if (databaseState.getActiveConnections().get() >= databaseState.getPoolSize() ||
                databaseState.getExtraQueryLatencyMs() > 0 ||
                databaseState.getQueryFailureRate() > 0) return HealthStatus.DEGRADED;
            return HealthStatus.HEALTHY;
        }
        if (service == LogicalService.PAYMENT) {
            if (!paymentState.isAvailable()) return HealthStatus.UNAVAILABLE;
            if (paymentState.isTimeoutEnabled() || paymentState.getFailureRate() > 0) return HealthStatus.DEGRADED;
            return HealthStatus.HEALTHY;
        }
        if (service == LogicalService.ORDERS) {
            if (getStatus(LogicalService.DATABASE) != HealthStatus.HEALTHY) return HealthStatus.DEGRADED;
            if (getStatus(LogicalService.PAYMENT) != HealthStatus.HEALTHY) return HealthStatus.DEGRADED;
            if (deploymentState.isDegradedDeployment() ||
                resourceState.getCpuUtilizationPercent() > 70.0 ||
                resourceState.getMemoryUtilizationPercent() > 70.0) return HealthStatus.DEGRADED;
            return HealthStatus.HEALTHY;
        }
        return manualStates.getOrDefault(service, HealthStatus.HEALTHY);
    }

    public void setState(LogicalService service, HealthStatus status) {
        manualStates.put(service, status);
    }

    /**
     * Return all service statuses as DTOs.
     */
    public List<ServiceStatusDto> getAllStatuses() {
        return Arrays.stream(LogicalService.values())
                .map(s -> new ServiceStatusDto(
                        s.name(),
                        getStatus(s).name(),
                        descriptionFor(s)
                ))
                .toList();
    }

    public ServiceStatusDto getStatusDto(LogicalService service) {
        return new ServiceStatusDto(
                service.name(),
                getStatus(service).name(),
                descriptionFor(service)
        );
    }

    /**
     * Reset all services to HEALTHY — called during simulator reset.
     */
    public void reset() {
        manualStates.clear();
    }

    private String descriptionFor(LogicalService service) {
        return switch (service) {
            case API_GATEWAY   -> "Routes incoming requests to internal services";
            case AUTH          -> "Validates user identity and session tokens";
            case ORDERS        -> "Manages order lifecycle and state transitions";
            case PAYMENT       -> "Processes payment transactions";
            case DATABASE      -> "Primary PostgreSQL persistence layer";
            case NOTIFICATION  -> "Sends order and payment notifications";
        };
    }
}
