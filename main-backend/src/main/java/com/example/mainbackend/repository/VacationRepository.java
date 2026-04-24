package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT COUNT(v) > 0 FROM Vacation v WHERE v.user.id = :userId " +
            "AND v.status.name = :statusName AND v.startDate <= :endDate AND v.endDate >= :startDate")
    boolean hasApprovedVacationOverlap(@Param("userId") Long userId, @Param("statusName") String statusName,
                                       @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
