package com.emergency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "telemetry")
@Getter
@Setter
@NoArgsConstructor
public class TelemetryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 50)
    private String requestId;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(nullable = false, length = 50)
    private String service;

    @Column(nullable = false, length = 100)
    private String operation;

    @Column(length = 255)
    private String endpoint;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(nullable = false)
    private boolean success = true;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(columnDefinition = "TEXT")
    private String message;

    public TelemetryRecord(String requestId, String service, String operation,
                           String endpoint, String httpMethod, Integer statusCode,
                           boolean success, long latencyMs, String errorType, String message) {
        this.requestId = requestId;
        this.service = service;
        this.operation = operation;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.success = success;
        this.latencyMs = latencyMs;
        this.errorType = errorType;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
