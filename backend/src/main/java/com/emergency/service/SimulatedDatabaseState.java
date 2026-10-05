package com.emergency.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concrete, observable simulated database state.
 * Reacts realistically to connection pool exhaustion, query latency, and outages.
 */
@Component
@Getter
@Setter
public class SimulatedDatabaseState {

    private int poolSize = 100;
    private final AtomicInteger activeConnections = new AtomicInteger(24);
    private final AtomicLong acquisitionFailures = new AtomicLong(0);
    private boolean available = true;
    private long extraQueryLatencyMs = 0;
    private double queryFailureRate = 0.0;

    public int getAvailableConnections() {
        return Math.max(0, poolSize - activeConnections.get());
    }

    public boolean tryAcquireConnection() {
        if (!available) {
            acquisitionFailures.incrementAndGet();
            return false;
        }
        int current = activeConnections.get();
        if (current >= poolSize) {
            acquisitionFailures.incrementAndGet();
            return false;
        }
        activeConnections.incrementAndGet();
        return true;
    }

    public void releaseConnection() {
        activeConnections.updateAndGet(c -> Math.max(0, c - 1));
    }

    public void reset() {
        this.poolSize = 100;
        this.activeConnections.set(24);
        this.acquisitionFailures.set(0);
        this.available = true;
        this.extraQueryLatencyMs = 0;
        this.queryFailureRate = 0.0;
    }
}
