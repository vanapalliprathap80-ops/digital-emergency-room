package com.emergency.repository;

import com.emergency.domain.Investigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestigationRepository extends JpaRepository<Investigation, String> {
    Optional<Investigation> findByIncidentId(String incidentId);
}
