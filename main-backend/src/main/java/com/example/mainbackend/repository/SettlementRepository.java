package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByWorkerId(Long workerId);
    List<Settlement> findByTaskId(Long taskId);
    List<Settlement> findBySettlementDateBetween(LocalDateTime start, LocalDateTime end);
}

