package com.example.mainbackend.repository;

import com.example.mainbackend.entity.VacationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VacationStatusRepository extends JpaRepository<VacationStatus, Long> {
    Optional<VacationStatus> findByName(String name);
}

