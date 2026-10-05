package com.emergency.service;

import com.emergency.domain.Incident;
import com.emergency.domain.IncidentStatus;
import com.emergency.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
public class IncidentStateMachine {

    public void transition(Incident incident, IncidentStatus targetStatus) {
        IncidentStatus current = incident.getStatus();
        if (!isValidTransition(current, targetStatus)) {
            throw new InvalidStateTransitionException(current, targetStatus);
        }

        incident.setStatus(targetStatus);
        if (targetStatus == IncidentStatus.ACTIVE && incident.getStartedAt() == null) {
            incident.setStartedAt(Instant.now());
        } else if (targetStatus == IncidentStatus.RESOLVED || targetStatus == IncidentStatus.RESET) {
            incident.setEndedAt(Instant.now());
        }
    }

    public boolean isValidTransition(IncidentStatus from, IncidentStatus to) {
        if (from == to) return true;

        return switch (from) {
            case CREATED -> to == IncidentStatus.INJECTED || to == IncidentStatus.RESET;
            case INJECTED -> to == IncidentStatus.ACTIVE || to == IncidentStatus.RESET;
            case ACTIVE -> to == IncidentStatus.RECOVERING || to == IncidentStatus.RESET || to == IncidentStatus.RESOLVED;
            case RECOVERING -> to == IncidentStatus.RESOLVED || to == IncidentStatus.ACTIVE || to == IncidentStatus.RESET;
            case RESOLVED, RESET -> false; // Terminal states
        };
    }
}
