package com.emergency;

import com.emergency.domain.Incident;
import com.emergency.domain.IncidentMode;
import com.emergency.domain.IncidentStatus;
import com.emergency.domain.Severity;
import com.emergency.domain.TrafficPattern;
import com.emergency.exception.InvalidStateTransitionException;
import com.emergency.service.IncidentStateMachine;
import com.emergency.service.LogicalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentStateMachineTest {

    private IncidentStateMachine stateMachine;
    private Incident incident;

    @BeforeEach
    void setUp() {
        stateMachine = new IncidentStateMachine();
        incident = new Incident("inc-test-01", "session-1", IncidentMode.MANUAL,
                Severity.HIGH, LogicalService.DATABASE, TrafficPattern.NORMAL, 42L);
    }

    @Test
    void testValidLifecycleTransition() {
        assertEquals(IncidentStatus.CREATED, incident.getStatus());

        stateMachine.transition(incident, IncidentStatus.INJECTED);
        assertEquals(IncidentStatus.INJECTED, incident.getStatus());

        stateMachine.transition(incident, IncidentStatus.ACTIVE);
        assertEquals(IncidentStatus.ACTIVE, incident.getStatus());
        assertNotNull(incident.getStartedAt());

        stateMachine.transition(incident, IncidentStatus.RECOVERING);
        assertEquals(IncidentStatus.RECOVERING, incident.getStatus());

        stateMachine.transition(incident, IncidentStatus.RESOLVED);
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertNotNull(incident.getEndedAt());
    }

    @Test
    void testActiveToResetTransition() {
        stateMachine.transition(incident, IncidentStatus.INJECTED);
        stateMachine.transition(incident, IncidentStatus.ACTIVE);
        stateMachine.transition(incident, IncidentStatus.RESET);

        assertEquals(IncidentStatus.RESET, incident.getStatus());
        assertNotNull(incident.getEndedAt());
    }

    @Test
    void testInvalidTransitionThrowsException() {
        // CREATED -> RESOLVED is not allowed
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transition(incident, IncidentStatus.RESOLVED));

        // Move to RESOLVED
        stateMachine.transition(incident, IncidentStatus.INJECTED);
        stateMachine.transition(incident, IncidentStatus.ACTIVE);
        stateMachine.transition(incident, IncidentStatus.RESOLVED);

        // Terminal state RESOLVED -> ACTIVE is not allowed
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transition(incident, IncidentStatus.ACTIVE));
    }
}
