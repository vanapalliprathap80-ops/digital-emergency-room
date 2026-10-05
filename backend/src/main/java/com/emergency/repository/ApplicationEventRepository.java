package com.emergency.repository;

import com.emergency.domain.ApplicationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {

    Page<ApplicationEvent> findAllByOrderByTimestampDesc(Pageable pageable);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM ApplicationEvent e")
    void deleteAllEvents();
}
