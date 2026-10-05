package com.emergency.repository;

import com.emergency.domain.GroundTruth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Internal-only Ground Truth repository.
 * MUST NEVER be injected into public controllers or observable DTOs.
 */
@Repository
public interface GroundTruthRepository extends JpaRepository<GroundTruth, Long> {
    Optional<GroundTruth> findByIncidentId(String incidentId);

    @Modifying
    @Query("DELETE FROM GroundTruth g")
    void deleteAllGroundTruth();
}
