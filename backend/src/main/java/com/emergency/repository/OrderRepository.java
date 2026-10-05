package com.emergency.repository;

import com.emergency.domain.Order;
import com.emergency.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(OrderStatus status);
}
