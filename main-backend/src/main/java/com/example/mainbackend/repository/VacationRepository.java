package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {
    List<Vacation> findByUserId(Long userId);
    List<Vacation> findByStartDateBetween(LocalDate start, LocalDate end);
    List<Vacation> findByStatus_Name(String statusName);
    List<Vacation> findAllByUser_Department_IdAndStatus_Name(Long departmentId, String statusName);
    List<Vacation> findAllByUser_Department_Id(Long departmentId);
}
