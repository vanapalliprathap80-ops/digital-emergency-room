package com.emergency.service;


import com.emergency.domain.FailureType;
import com.emergency.domain.Order;
import com.emergency.domain.OrderStatus;
import com.emergency.dto.CreateOrderRequest;
import com.emergency.dto.OrderResponse;
import com.emergency.events.EventPublisher;
import com.emergency.events.EventType;
import com.emergency.exception.OrderNotFoundException;
import com.emergency.exception.SimulatorException;
import com.emergency.repository.OrderRepository;
import com.emergency.telemetry.TelemetryRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final TelemetryRecorder telemetryRecorder;
    private final EventPublisher eventPublisher;
    private final AuthService authService;
    private final PaymentService paymentService;
    private final FailureEngine failureEngine;
    private final SimulatedDatabaseState databaseState;
    private final SimulatedDeploymentState deploymentState;
    private final SimulatedResourceState resourceState;
    private final SymptomLogger symptomLogger;
    private final Random random = new Random();

    public OrderService(OrderRepository orderRepository,
                        TelemetryRecorder telemetryRecorder,
                        EventPublisher eventPublisher,
                        AuthService authService,
                        PaymentService paymentService,
                        FailureEngine failureEngine,
                        SimulatedDatabaseState databaseState,
                        SimulatedDeploymentState deploymentState,
                        SimulatedResourceState resourceState,
                        SymptomLogger symptomLogger) {
        this.orderRepository = orderRepository;
        this.telemetryRecorder = telemetryRecorder;
        this.eventPublisher = eventPublisher;
        this.authService = authService;
        this.paymentService = paymentService;
        this.failureEngine = failureEngine;
        this.databaseState = databaseState;
        this.deploymentState = deploymentState;
        this.resourceState = resourceState;
        this.symptomLogger = symptomLogger;
    }

    @Transactional
    public OrderResponse createOrder(String requestId, CreateOrderRequest request) {
        log.info("Creating order for userId={} requestId={}", request.userId(), requestId);
        ActiveFailureState active = failureEngine.getActiveFailure();

        // --- 1. API GATEWAY ingress span ---
        long gatewayLatency = TelemetryRecorder.simulateLatency(5, 15);
        if (active != null && active.getFailureType() == FailureType.API_HIGH_LATENCY) {
            gatewayLatency += active.getSeverity().getLatencyInjectionMs();
        }
        if (active != null && active.getFailureType() == FailureType.API_5XX) {
            telemetryRecorder.record(requestId, LogicalService.API_GATEWAY.name(), "ROUTE_REQUEST",
                    "/api/orders", "POST", 500, gatewayLatency, "GATEWAY_5XX", "Internal gateway error 500");
            symptomLogger.error(LogicalService.API_GATEWAY, "HTTP 500 returned during upstream routing to order service", requestId);
            throw new SimulatorException("API Gateway failed with 500 Internal Server Error");
        }
        telemetryRecorder.record(requestId, LogicalService.API_GATEWAY.name(), "ROUTE_REQUEST",
                "/api/orders", "POST", 200, gatewayLatency);

        // --- 2. AUTH span ---
        authService.authenticate(requestId, request.userId());

        // --- 3. ORDERS validation + deployment check ---
        long ordersLatency = TelemetryRecorder.simulateLatency(15, 40);
        if (resourceState.getGcPauseLatencyMs() > 0) {
            ordersLatency += resourceState.getGcPauseLatencyMs();
        }

        if (deploymentState.isDegradedDeployment() && random.nextDouble() < deploymentState.getFailureRate()) {
            telemetryRecorder.record(requestId, LogicalService.ORDERS.name(), "CREATE_ORDER",
                    "/api/orders", "POST", 500, ordersLatency, "RUNTIME_EXCEPTION",
                    "NullPointerException in OrderWorkflow " + deploymentState.getVersion());
            symptomLogger.error(LogicalService.ORDERS,
                    "Unhandled NullPointerException in OrderWorkflow handler version " + deploymentState.getVersion(), requestId);
            throw new SimulatorException("Order processing failed in service version " + deploymentState.getVersion());
        }
        telemetryRecorder.record(requestId, LogicalService.ORDERS.name(), "CREATE_ORDER",
                "/api/orders", "POST", 200, ordersLatency);

        // --- 4. DATABASE span (persist order) ---
        long dbLatency = TelemetryRecorder.simulateLatency(5, 25) + databaseState.getExtraQueryLatencyMs();
        if (!databaseState.tryAcquireConnection()) {
            telemetryRecorder.record(requestId, LogicalService.DATABASE.name(), "PERSIST_ORDER",
                    "/internal/db/orders", "WRITE", 500, 5000, "CONNECTION_POOL_EXHAUSTED",
                    "Timed out waiting for connection from pool");
            symptomLogger.error(LogicalService.DATABASE,
                    "Failed to acquire connection from pool within 5000ms: poolSize=" + databaseState.getPoolSize(), requestId);
            throw new SimulatorException("Database connection pool exhausted");
        }

        Order order;
        try {
            if (random.nextDouble() < databaseState.getQueryFailureRate()) {
                telemetryRecorder.record(requestId, LogicalService.DATABASE.name(), "PERSIST_ORDER",
                        "/internal/db/orders", "WRITE", 500, dbLatency, "SQL_ERROR", "Lock wait timeout exceeded");
                symptomLogger.error(LogicalService.DATABASE, "Deadlock / lock wait timeout during row insertion into table 'orders'", requestId);
                throw new SimulatorException("Database query failed");
            }

            telemetryRecorder.record(requestId, LogicalService.DATABASE.name(), "PERSIST_ORDER",
                    "/internal/db/orders", "WRITE", 200, dbLatency);

            order = new Order(request.userId(), request.amount());
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order = orderRepository.save(order);

        } finally {
            databaseState.releaseConnection();
        }

        eventPublisher.publish(EventType.ORDER_CREATED, LogicalService.ORDERS, requestId,
                String.format("Order %s created for userId=%s amount=%.2f",
                        order.getId(), request.userId(), request.amount().doubleValue()),
                String.format("{\"orderId\":\"%s\",\"userId\":\"%s\",\"amount\":%s}",
                        order.getId(), request.userId(), request.amount()));

        // --- 5. PAYMENT span ---
        try {
            paymentService.processPayment(requestId, order.getId().toString(),
                    request.userId(), request.amount().doubleValue());
            order.setStatus(OrderStatus.PAID);
            order = orderRepository.save(order);
        } catch (Exception e) {
            order.setStatus(OrderStatus.FAILED);
            order = orderRepository.save(order);
            log.warn("Payment failed for order {}: {}", order.getId(), e.getMessage());
            throw e;
        }

        // --- 6. Final API GATEWAY response span ---
        long responseLatency = TelemetryRecorder.simulateLatency(2, 8);
        telemetryRecorder.record(requestId, LogicalService.API_GATEWAY.name(), "SEND_RESPONSE",
                "/api/orders", "POST", 201, responseLatency);

        log.info("Order {} created and paid requestId={}", order.getId(), requestId);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
