package com.emergency.repository;

import com.emergency.domain.RemediationAction;
import com.emergency.domain.RemediationActionType;
import com.emergency.domain.RemediationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemediationRepository extends JpaRepository<RemediationAction, String> {

    List<RemediationAction> findByIncidentIdOrderByProposedAtDesc(String incidentId);

    boolean existsByIncidentIdAndActionTypeAndStatusIn(
            String incidentId,
            RemediationActionType actionType,
            List<RemediationStatus> statuses);
}
