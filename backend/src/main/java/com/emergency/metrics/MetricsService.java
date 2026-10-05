package com.emergency.metrics;

import com.emergency.dto.ServiceMetricsDto;
import com.emergency.dto.SystemMetricsDto;
import com.emergency.repository.TelemetryRepository;
import com.emergency.service.HealthStatus;
import com.emergency.service.LogicalService;
import com.emergency.service.ServiceRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Calculates all metrics from actual telemetry data.
 * P95 is computed from sorted latency values — not approximated from average.
 */
@Service
public class MetricsService {

    private final TelemetryRepository telemetryRepository;
    private final ServiceRegistry serviceRegistry; // Keeping it for fallback or reference if needed, but will avoid for dynamic metrics

    public MetricsService(TelemetryRepository telemetryRepository, ServiceRegistry serviceRegistry) {
        this.telemetryRepository = telemetryRepository;
        this.serviceRegistry = serviceRegistry;
    }

    public SystemMetricsDto getSystemMetrics() {
        long total = telemetryRepository.count();
        long errors = telemetryRepository.countBySuccessFalse();
        double errorRate = total == 0 ? 0.0 : (double) errors / total * 100.0;

        List<Long> sortedLatencies = telemetryRepository.findAllLatenciesSorted();
        double avgLatency = sortedLatencies.isEmpty() ? 0 :
                sortedLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95Latency = calculateP95(sortedLatencies);

        double requestsPerMinute = calculateRequestsPerMinute();

        List<ServiceMetricsDto> serviceMetrics = Arrays.stream(LogicalService.values())
                .map(this::getServiceMetrics)
                .toList();

        return new SystemMetricsDto(total, errors, errorRate, avgLatency, p95Latency,
                requestsPerMinute, serviceMetrics);
    }

    public List<ServiceMetricsDto> getAllServiceMetrics() {
        return Arrays.stream(LogicalService.values())
                .map(this::getServiceMetrics)
                .toList();
    }

    public ServiceMetricsDto getServiceMetrics(LogicalService service) {
        String serviceName = service.name();
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);

        List<Long> latencies = telemetryRepository.findLatenciesByServiceSorted(serviceName);
        long totalAll = latencies.size();

        long errorsAll = telemetryRepository.countByServiceAndSuccessFalse(serviceName);

        double errorRate = totalAll == 0 ? 0.0 : (double) errorsAll / totalAll * 100.0;
        double avgLatency = latencies.isEmpty() ? 0 :
                latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95Latency = calculateP95(latencies);

        long recentCount = telemetryRepository.countByServiceAndTimestampAfter(serviceName, oneMinuteAgo);

        HealthStatus health = calculateHealthFromTelemetry(service, serviceName, oneMinuteAgo, recentCount);

        return new ServiceMetricsDto(serviceName, health.name(), totalAll, errorsAll,
                errorRate, avgLatency, p95Latency, recentCount);
    }

    private HealthStatus calculateHealthFromTelemetry(LogicalService service, String serviceName, Instant oneMinuteAgo, long recentCount) {
        if (recentCount == 0) {
            // No recent traffic, fallback to service registry to check for manual overrides like UNAVAILABLE
            return serviceRegistry.getStatus(service);
        }

        long recentErrors = telemetryRepository.countByServiceAndSuccessFalseAndTimestampAfter(serviceName, oneMinuteAgo);
        double recentErrorRate = (double) recentErrors / recentCount * 100.0;

        if (recentErrorRate > 10.0) {
            return HealthStatus.DEGRADED;
        } else if (recentErrorRate > 0.0) {
            return HealthStatus.DEGRADED;
        }

        List<Long> recentLatencies = telemetryRepository.findLatenciesByServiceAndTimestampAfterSorted(serviceName, oneMinuteAgo);
        long p95 = calculateP95(recentLatencies);

        if (p95 > 2000) {
            return HealthStatus.DEGRADED;
        }

        return HealthStatus.HEALTHY;
    }

    /**
     * Correctly computes P95 from a sorted list of latency values.
     */
    public static long calculateP95(List<Long> sortedLatencies) {
        if (sortedLatencies == null || sortedLatencies.isEmpty()) {
            return 0L;
        }
        if (sortedLatencies.size() == 1) {
            return sortedLatencies.get(0);
        }
        int index = (int) Math.ceil(0.95 * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(index, sortedLatencies.size() - 1));
        return sortedLatencies.get(index);
    }

    private double calculateRequestsPerMinute() {
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        return telemetryRepository.countByTimestampAfter(oneMinuteAgo);
    }
}
