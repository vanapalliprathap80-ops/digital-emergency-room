package com.emergency.repository;

import com.emergency.domain.TelemetryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryRecord, Long> {

    Page<TelemetryRecord> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<TelemetryRecord> findByServiceOrderByTimestampDesc(String service, Pageable pageable);

    List<TelemetryRecord> findByRequestIdOrderByTimestampAsc(String requestId);

    // System-wide counts
    long countByTimestampAfter(Instant since);
    long countBySuccessFalse();
    long countBySuccessTrue();

    // Per-service counts
    long countByServiceAndSuccessFalse(String service);
    long countByServiceAndTimestampAfter(String service, Instant since);
    long countByServiceAndSuccessFalseAndTimestampAfter(String service, Instant since);

    // Recent spans since a timestamp
    @Query("SELECT t FROM TelemetryRecord t WHERE t.timestamp >= :since ORDER BY t.timestamp DESC")
    List<TelemetryRecord> findRecentSince(@Param("since") Instant since);

    // Sorted latencies for P95 — system-wide and per-service
    @Query("SELECT t.latencyMs FROM TelemetryRecord t ORDER BY t.latencyMs ASC")
    List<Long> findAllLatenciesSorted();

    @Query("SELECT t.latencyMs FROM TelemetryRecord t WHERE t.service = :service ORDER BY t.latencyMs ASC")
    List<Long> findLatenciesByServiceSorted(@Param("service") String service);

    @Query("SELECT t.latencyMs FROM TelemetryRecord t WHERE t.service = :service AND t.timestamp >= :since ORDER BY t.latencyMs ASC")
    List<Long> findLatenciesByServiceAndTimestampAfterSorted(@Param("service") String service, @Param("since") Instant since);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM TelemetryRecord t")
    void deleteAllTelemetry();
}
