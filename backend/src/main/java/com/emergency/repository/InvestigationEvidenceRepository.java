package com.emergency.repository;

import com.emergency.domain.InvestigationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvestigationEvidenceRepository extends JpaRepository<InvestigationEvidence, Long> {
    List<InvestigationEvidence> findByInvestigationIdOrderByCreatedAtAsc(String investigationId);
    long countByInvestigationId(String investigationId);
}
