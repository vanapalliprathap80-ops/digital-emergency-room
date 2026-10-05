package com.emergency.domain;

import com.emergency.service.LogicalService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "simulator_logs")
@Getter
@Setter
@NoArgsConstructor
public class SimulatorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LogicalService service;

    @Column(nullable = false, length = 20)
    private String level = "INFO";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public SimulatorLog(LogicalService service, String level, String message, String requestId, String metadata) {
        this.service = service;
        this.level = level != null ? level : "INFO";
        this.message = message;
        this.requestId = requestId;
        this.metadata = metadata;
        this.timestamp = Instant.now();
    }
}
