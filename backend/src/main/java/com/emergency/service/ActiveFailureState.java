package com.emergency.service;

import com.emergency.domain.FailureComponent;
import com.emergency.domain.FailureType;
import com.emergency.domain.Severity;
import com.emergency.domain.TrafficPattern;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ActiveFailureState {
    private final String incidentId;
    private final FailureType failureType;
    private final FailureComponent component;
    private final Severity severity;
    private final LogicalService affectedService;
    private final TrafficPattern trafficPattern;
    private final Instant startTime;

    public ActiveFailureState(String incidentId, FailureType failureType,
                              FailureComponent component, Severity severity,
                              LogicalService affectedService, TrafficPattern trafficPattern) {
        this.incidentId = incidentId;
        this.failureType = failureType;
        this.component = component;
        this.severity = severity;
        this.affectedService = affectedService;
        this.trafficPattern = trafficPattern;
        this.startTime = Instant.now();
    }
}
