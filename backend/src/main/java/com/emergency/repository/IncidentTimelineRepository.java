package com.emergency.repository;

import com.emergency.domain.IncidentTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentTimelineRepository extends JpaRepository<IncidentTimelineEvent, Long> {

    List<IncidentTimelineEvent> findByIncidentIdOrderByTimestampAsc(String incidentId);

    @Modifying
    @Query("DELETE FROM IncidentTimelineEvent t")
    void deleteAllTimelineEvents();
}
