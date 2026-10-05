package com.emergency.exception;

import com.emergency.domain.IncidentStatus;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(IncidentStatus from, IncidentStatus to) {
        super(String.format("Invalid incident state transition from %s to %s", from, to));
    }
}
