package com.emergency.domain;

import com.emergency.service.LogicalService;

public enum FailureType {
    // Database
    DATABASE_UNAVAILABLE(LogicalService.DATABASE, FailureComponent.PERSISTENCE, "Database connection dropped / port closed"),
    DATABASE_CONNECTION_POOL_EXHAUSTION(LogicalService.DATABASE, FailureComponent.CONNECTION_POOL, "All connections in Hikari pool consumed"),
    DATABASE_QUERY_LATENCY(LogicalService.DATABASE, FailureComponent.QUERY_EXECUTION, "Slow query execution / lock contention"),
    DATABASE_QUERY_FAILURE(LogicalService.DATABASE, FailureComponent.QUERY_EXECUTION, "Query syntax/constraint violation errors"),

    // Payment
    PAYMENT_TIMEOUT(LogicalService.PAYMENT, FailureComponent.PAYMENT_PROVIDER_SIMULATION, "Third-party payment gateway timeout"),
    PAYMENT_SERVICE_UNAVAILABLE(LogicalService.PAYMENT, FailureComponent.GATEWAY, "Payment microservice unavailable (503)"),
    PAYMENT_FAILURE_RATE(LogicalService.PAYMENT, FailureComponent.PAYMENT_PROVIDER_SIMULATION, "Elevated transaction decline/failure rate"),

    // API
    API_HIGH_LATENCY(LogicalService.API_GATEWAY, FailureComponent.INGRESS_ROUTER, "Ingress latency spike across all endpoints"),
    API_5XX(LogicalService.API_GATEWAY, FailureComponent.INGRESS_ROUTER, "HTTP 500/502 internal server errors on gateway"),

    // Resource Pressure
    CPU_PRESSURE(LogicalService.ORDERS, FailureComponent.CPU_SUBSYSTEM, "Elevated compute utilization causing thread starvation"),
    MEMORY_PRESSURE(LogicalService.ORDERS, FailureComponent.MEMORY_SUBSYSTEM, "Elevated memory/GC pressure causing request latency and timeouts"),

    // Deployment
    BAD_DEPLOYMENT(LogicalService.ORDERS, FailureComponent.DEPLOYMENT, "Faulty release v1.1 causing runtime exceptions");

    private final LogicalService affectedService;
    private final FailureComponent component;
    private final String description;

    FailureType(LogicalService affectedService, FailureComponent component, String description) {
        this.affectedService = affectedService;
        this.component = component;
        this.description = description;
    }

    public LogicalService getAffectedService() {
        return affectedService;
    }

    public FailureComponent getComponent() {
        return component;
    }

    public String getDescription() {
        return description;
    }
}
