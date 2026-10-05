package com.emergency.service;

import org.springframework.stereotype.Service;

@Service
public class EnvironmentContext {
    private Environment currentEnvironment = Environment.SIMULATOR;

    public Environment getCurrentEnvironment() {
        return currentEnvironment;
    }

    public void setCurrentEnvironment(Environment currentEnvironment) {
        this.currentEnvironment = currentEnvironment;
    }

    public enum Environment {
        SIMULATOR,
        PRODUCTION
    }
}
