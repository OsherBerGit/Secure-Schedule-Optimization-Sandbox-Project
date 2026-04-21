package com.example.mainbackend.repository;

import com.example.mainbackend.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementStatusRepository extends JpaRepository<SettlementStatus, Long> {
    Optional<SettlementStatus> findByName(String name);
}
