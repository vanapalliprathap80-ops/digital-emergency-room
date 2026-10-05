package com.emergency.repository;

import com.emergency.domain.Incident;
import com.emergency.domain.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByIncidentId(String incidentId);

    Optional<Incident> findBySessionIdAndStatusIn(String sessionId, List<IncidentStatus> statuses);

    List<Incident> findByStatus(IncidentStatus status);

    Page<Incident> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Incident> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE i.status = 'ACTIVE' AND i.startedAt < :cutoff")
    List<Incident> findAbandonedIncidents(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM Incident i")
    void deleteAllIncidents();
}
