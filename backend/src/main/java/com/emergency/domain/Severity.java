package com.emergency.domain;

public enum Severity {
    LOW(0.2, 100),
    MEDIUM(0.5, 400),
    HIGH(0.8, 1500),
    CRITICAL(1.0, 4000);

    private final double failureRate;
    private final long latencyInjectionMs;

    Severity(double failureRate, long latencyInjectionMs) {
        this.failureRate = failureRate;
        this.latencyInjectionMs = latencyInjectionMs;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public long getLatencyInjectionMs() {
        return latencyInjectionMs;
    }
}
