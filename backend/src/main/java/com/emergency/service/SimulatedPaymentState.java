package com.emergency.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class SimulatedPaymentState {
    private boolean available = true;
    private boolean timeoutEnabled = false;
    private double failureRate = 0.0;
    private long timeoutDurationMs = 8000;

    public void reset() {
        this.available = true;
        this.timeoutEnabled = false;
        this.failureRate = 0.0;
        this.timeoutDurationMs = 8000;
    }
}
