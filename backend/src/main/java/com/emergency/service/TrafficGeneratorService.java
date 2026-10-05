package com.emergency.service;

import com.emergency.dto.CreateOrderRequest;
import com.emergency.dto.TrafficGenerateResponse;
import com.emergency.telemetry.TelemetryRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes actual simulated requests flowing through the complete service stack.
 * This is NOT fake telemetry insertion — each generated request calls OrderService.
 */
@Service
public class TrafficGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(TrafficGeneratorService.class);

    private static final String[] USER_IDS = {
            "user-alice", "user-bob", "user-charlie", "user-diana",
            "user-eve", "user-frank", "user-grace", "user-henry"
    };

    private final OrderService orderService;
    private final Random random = new Random();

    public TrafficGeneratorService(OrderService orderService) {
        this.orderService = orderService;
    }

    public TrafficGenerateResponse generate(int count) {
        long start = System.currentTimeMillis();
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        log.info("Starting traffic generation: {} requests", count);

        for (int i = 0; i < count; i++) {
            try {
                String requestId = TelemetryRecorder.generateRequestId();
                String userId = USER_IDS[random.nextInt(USER_IDS.length)];
                BigDecimal amount = BigDecimal.valueOf(10 + random.nextInt(991))
                        .setScale(2, RoundingMode.HALF_UP);

                orderService.createOrder(requestId, new CreateOrderRequest(userId, amount));
                succeeded.incrementAndGet();
            } catch (Exception ex) {
                log.warn("Traffic request {} failed: {}", i, ex.getMessage());
                failed.incrementAndGet();
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Traffic generation complete: {}/{} succeeded in {}ms", succeeded, count, duration);

        return new TrafficGenerateResponse(count, succeeded.get() + failed.get(),
                succeeded.get(), failed.get(), duration);
    }
}
