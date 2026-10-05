package com.emergency.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class SimulatedResourceState {
    private double cpuUtilizationPercent = 14.5;
    private double memoryUtilizationPercent = 28.2;
    private long gcPauseLatencyMs = 0;

    public void reset() {
        this.cpuUtilizationPercent = 14.5;
        this.memoryUtilizationPercent = 28.2;
        this.gcPauseLatencyMs = 0;
    }
}
