package com.emergency.dto;

public record SimulatedSystemStateDto(
        int dbPoolSize,
        int dbActiveConnections,
        int dbAvailableConnections,
        long dbAcquisitionFailures,
        boolean dbAvailable,
        double cpuUtilizationPercent,
        double memoryUtilizationPercent,
        String deploymentVersion,
        String deploymentStatus,
        boolean paymentAvailable,
        boolean hasActiveIncident,
        String activeIncidentId
) {}
