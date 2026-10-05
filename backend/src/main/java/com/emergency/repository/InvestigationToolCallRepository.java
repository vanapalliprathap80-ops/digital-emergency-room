package com.emergency.repository;

import com.emergency.domain.InvestigationToolCall;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvestigationToolCallRepository extends JpaRepository<InvestigationToolCall, String> {
    List<InvestigationToolCall> findByInvestigationIdOrderByTimestampAsc(String investigationId);
}
