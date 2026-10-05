package com.emergency.service;

import com.emergency.domain.SimulatorLog;
import com.emergency.repository.SimulatorLogRepository;
import org.springframework.stereotype.Service;

@Service
public class SymptomLogger {

    private final SimulatorLogRepository logRepository;

    public SymptomLogger(SimulatorLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void log(LogicalService service, String level, String message, String requestId, String metadata) {
        SimulatorLog entry = new SimulatorLog(service, level, message, requestId, metadata);
        logRepository.save(entry);
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void info(LogicalService service, String message, String requestId) {
        SimulatorLog entry = new SimulatorLog(service, "INFO", message, requestId, null);
        logRepository.save(entry);
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void warn(LogicalService service, String message, String requestId) {
        SimulatorLog entry = new SimulatorLog(service, "WARN", message, requestId, null);
        logRepository.save(entry);
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void error(LogicalService service, String message, String requestId) {
        SimulatorLog entry = new SimulatorLog(service, "ERROR", message, requestId, null);
        logRepository.save(entry);
    }
}
