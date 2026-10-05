package com.emergency.repository;

import com.emergency.domain.SimulatorLog;
import com.emergency.service.LogicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulatorLogRepository extends JpaRepository<SimulatorLog, Long> {

    Page<SimulatorLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<SimulatorLog> findByServiceOrderByTimestampDesc(LogicalService service, Pageable pageable);

    Page<SimulatorLog> findByLevelOrderByTimestampDesc(String level, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SimulatorLog l")
    void deleteAllLogs();
}
