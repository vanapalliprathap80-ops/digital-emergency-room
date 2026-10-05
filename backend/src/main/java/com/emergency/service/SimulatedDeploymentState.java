package com.emergency.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class SimulatedDeploymentState {
    private String version = "v1.0.0";
    private String deploymentStatus = "HEALTHY";
    private boolean degradedDeployment = false;
    private double failureRate = 0.0;

    public void reset() {
        this.version = "v1.0.0";
        this.deploymentStatus = "HEALTHY";
        this.degradedDeployment = false;
        this.failureRate = 0.0;
    }
}
