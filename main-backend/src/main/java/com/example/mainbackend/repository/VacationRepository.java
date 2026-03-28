package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {
    List<Vacation> findByWorkerId(Long workerId);
    List<Vacation> findByStartDateBetween(LocalDate start, LocalDate end);
    List<Vacation> findByStatus_Name(String statusName);
    List<Vacation> findAllByWorker_Department_IdAndStatus_Name(Long departmentId, String statusName);
}
