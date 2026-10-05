package com.emergency;

import com.emergency.dto.CreateOrderRequest;
import com.emergency.dto.OrderResponse;
import com.emergency.domain.OrderStatus;
import com.emergency.metrics.MetricsService;
import com.emergency.repository.TelemetryRepository;
import com.emergency.service.LogicalService;
import com.emergency.service.OrderService;
import com.emergency.service.ServiceRegistry;
import com.emergency.service.HealthStatus;
import com.emergency.topology.TopologyService;
import com.emergency.dto.TopologyResponse;
import com.emergency.dto.SystemMetricsDto;
import com.emergency.dto.ServiceMetricsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase1IntegrationTests {

    @Autowired OrderService orderService;
    @Autowired TelemetryRepository telemetryRepository;
    @Autowired MetricsService metricsService;
    @Autowired ServiceRegistry serviceRegistry;
    @Autowired TopologyService topologyService;

    // ===== ORDERS =====

    @Test
    @DisplayName("Create order returns PAID order with valid ID")
    void createOrder_returnsValidOrder() {
        CreateOrderRequest req = new CreateOrderRequest("user-test", BigDecimal.valueOf(99.99));
        OrderResponse response = orderService.createOrder("req-test-001", req);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.userId()).isEqualTo("user-test");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("Get order by ID returns correct order")
    void getOrderById_returnsCorrectOrder() {
        CreateOrderRequest req = new CreateOrderRequest("user-alice", BigDecimal.valueOf(50.00));
        OrderResponse created = orderService.createOrder("req-test-002", req);

        OrderResponse found = orderService.getOrderById(created.id());
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.userId()).isEqualTo("user-alice");
    }

    @Test
    @DisplayName("Get orders returns paginated results")
    void getOrders_returnsPaginatedResults() {
        orderService.createOrder("req-test-003", new CreateOrderRequest("user-a", BigDecimal.valueOf(10)));
        orderService.createOrder("req-test-004", new CreateOrderRequest("user-b", BigDecimal.valueOf(20)));

        Page<OrderResponse> page = orderService.getOrders(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Get order with unknown ID throws OrderNotFoundException")
    void getOrderById_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> orderService.getOrderById(UUID.randomUUID()))
                .isInstanceOf(com.emergency.exception.OrderNotFoundException.class);
    }

    // ===== TELEMETRY =====

    @Test
    @DisplayName("Creating an order generates telemetry rows")
    void createOrder_generatesTelemetry() {
        long before = telemetryRepository.count();
        orderService.createOrder("req-tel-001", new CreateOrderRequest("user-t", BigDecimal.valueOf(75)));
        long after = telemetryRepository.count();

        assertThat(after).isGreaterThan(before);
    }

    @Test
    @DisplayName("Request ID is preserved across all telemetry spans")
    void createOrder_requestIdPreserved() {
        String requestId = "req-corr-001";
        orderService.createOrder(requestId, new CreateOrderRequest("user-corr", BigDecimal.valueOf(100)));

        var spans = telemetryRepository.findByRequestIdOrderByTimestampAsc(requestId);
        assertThat(spans).isNotEmpty();
        assertThat(spans).allMatch(t -> requestId.equals(t.getRequestId()));
    }

    @Test
    @DisplayName("All six logical services appear in telemetry for one order")
    void createOrder_allServicesInTelemetry() {
        String requestId = "req-svc-001";
        orderService.createOrder(requestId, new CreateOrderRequest("user-svc", BigDecimal.valueOf(150)));

        var spans = telemetryRepository.findByRequestIdOrderByTimestampAsc(requestId);
        var services = spans.stream().map(t -> t.getService()).distinct().toList();

        assertThat(services).contains(
                LogicalService.API_GATEWAY.name(),
                LogicalService.AUTH.name(),
                LogicalService.ORDERS.name(),
                LogicalService.DATABASE.name(),
                LogicalService.PAYMENT.name(),
                LogicalService.NOTIFICATION.name()
        );
    }

    @Test
    @DisplayName("Latency is recorded as non-negative value in every span")
    void createOrder_latencyRecorded() {
        String requestId = "req-lat-001";
        orderService.createOrder(requestId, new CreateOrderRequest("user-lat", BigDecimal.valueOf(200)));

        var spans = telemetryRepository.findByRequestIdOrderByTimestampAsc(requestId);
        assertThat(spans).isNotEmpty();
        assertThat(spans).allMatch(t -> t.getLatencyMs() >= 0);
    }

    // ===== METRICS =====

    @Test
    @DisplayName("System metrics return request count matching actual telemetry")
    void systemMetrics_requestCountMatchesTelemetry() {
        orderService.createOrder("req-m-001", new CreateOrderRequest("user-m", BigDecimal.valueOf(30)));
        orderService.createOrder("req-m-002", new CreateOrderRequest("user-m", BigDecimal.valueOf(40)));

        long telemetryCount = telemetryRepository.count();
        SystemMetricsDto metrics = metricsService.getSystemMetrics();

        assertThat(metrics.totalRequests()).isEqualTo(telemetryCount);
    }

    @Test
    @DisplayName("Error rate is 0.0 when all requests succeed in Phase 1")
    void systemMetrics_errorRateZeroInPhase1() {
        orderService.createOrder("req-err-001", new CreateOrderRequest("user-e", BigDecimal.valueOf(55)));
        SystemMetricsDto metrics = metricsService.getSystemMetrics();

        assertThat(metrics.errorRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("P95 latency is >= average latency")
    void systemMetrics_p95GreaterThanOrEqualToAvg() {
        orderService.createOrder("req-p95-001", new CreateOrderRequest("user-p", BigDecimal.valueOf(80)));
        orderService.createOrder("req-p95-002", new CreateOrderRequest("user-p", BigDecimal.valueOf(90)));

        SystemMetricsDto metrics = metricsService.getSystemMetrics();
        assertThat(metrics.p95LatencyMs()).isGreaterThanOrEqualTo((long) metrics.averageLatencyMs());
    }

    @Test
    @DisplayName("Service metrics are returned for all six services")
    void serviceMetrics_allSixServicesPresent() {
        List<ServiceMetricsDto> serviceMetrics = metricsService.getAllServiceMetrics();
        assertThat(serviceMetrics).hasSize(6);
        assertThat(serviceMetrics.stream().map(ServiceMetricsDto::service).toList())
                .contains("API_GATEWAY", "AUTH", "ORDERS", "PAYMENT", "DATABASE", "NOTIFICATION");
    }

    // ===== SERVICES / HEALTH =====

    @Test
    @DisplayName("All services are HEALTHY initially")
    void allServicesInitiallyHealthy() {
        serviceRegistry.reset();
        for (LogicalService service : LogicalService.values()) {
            assertThat(serviceRegistry.getStatus(service)).isEqualTo(HealthStatus.HEALTHY);
        }
    }

    @Test
    @DisplayName("Service registry returns all six statuses")
    void serviceRegistry_returnsAllStatuses() {
        assertThat(serviceRegistry.getAllStatuses()).hasSize(6);
    }

    @Test
    @DisplayName("Service registry reset restores all to HEALTHY")
    void serviceRegistry_resetRestoresHealthy() {
        serviceRegistry.setState(LogicalService.DATABASE, HealthStatus.DEGRADED);
        serviceRegistry.reset();
        assertThat(serviceRegistry.getStatus(LogicalService.DATABASE)).isEqualTo(HealthStatus.HEALTHY);
    }

    // ===== TOPOLOGY =====

    @Test
    @DisplayName("Topology returns six nodes")
    void topology_returnsSixNodes() {
        TopologyResponse topology = topologyService.getTopology();
        assertThat(topology.nodes()).hasSize(6);
    }

    @Test
    @DisplayName("Topology returns expected edges including API_GATEWAY -> ORDERS")
    void topology_returnsExpectedEdges() {
        TopologyResponse topology = topologyService.getTopology();
        assertThat(topology.edges()).isNotEmpty();

        boolean hasGatewayToOrders = topology.edges().stream()
                .anyMatch(e -> "API_GATEWAY".equals(e.source()) && "ORDERS".equals(e.target()));
        assertThat(hasGatewayToOrders).isTrue();
    }

    // ===== P95 CALCULATION =====

    @Test
    @DisplayName("P95 calculation is correct for known dataset")
    void p95_correctCalculation() {
        // 10 values sorted: 10..100; 95th percentile = index ceil(0.95*10)-1 = 9 => 100
        List<Long> latencies = List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L);
        long p95 = MetricsService.calculateP95(latencies);
        assertThat(p95).isEqualTo(100L);
    }

    @Test
    @DisplayName("P95 returns 0 for empty list")
    void p95_emptyListReturnsZero() {
        assertThat(MetricsService.calculateP95(List.of())).isEqualTo(0L);
    }

    @Test
    @DisplayName("P95 returns single value for single-element list")
    void p95_singleElement() {
        assertThat(MetricsService.calculateP95(List.of(42L))).isEqualTo(42L);
    }

    // ===== TRAFFIC (light version — generates 10 real requests) =====

    @Test
    @DisplayName("Generating 10 requests creates actual telemetry")
    void trafficGeneration_10requests_createsTelemetry() {
        long before = telemetryRepository.count();

        com.emergency.service.TrafficGeneratorService trafficService =
                new com.emergency.service.TrafficGeneratorService(orderService);
        var result = trafficService.generate(10);

        assertThat(result.succeeded()).isEqualTo(10);
        assertThat(telemetryRepository.count()).isGreaterThan(before);
    }
}
