package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    boolean existsByTask_IdAndUser_Id(Long taskId, Long userId);

    // Eagerly load all associations to prevent LazyInitializationException outside transactions.
    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.user JOIN FETCH s.status")
    List<Settlement> findAllWithDetails();

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.user JOIN FETCH s.status WHERE s.id = :id")
    Optional<Settlement> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.user JOIN FETCH s.status WHERE s.user.id = :userId")
    List<Settlement> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.user JOIN FETCH s.status WHERE s.task.id = :taskId")
    List<Settlement> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.user JOIN FETCH s.status WHERE s.task.id IN :taskIds")
    List<Settlement> findByTaskIdIn(@Param("taskIds") Collection<Long> taskIds);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.user JOIN FETCH s.status WHERE s.settlementDate BETWEEN :start AND :end")
    List<Settlement> findBySettlementDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.user WHERE s.user.nationalId = :nationalId")
    List<Settlement> findByUser_NationalId(@Param("nationalId") String nationalId);

    @Query("SELECT s.user.id, COUNT(s) FROM Settlement s WHERE s.user.id IN :userIds AND s.status.name IN :statusNames GROUP BY s.user.id")
    List<Object[]> countActiveSettlementsUserIds(@Param("userIds") Collection<Long> userIds, @Param("statusNames") Collection<String> statusNames);

    @Query("SELECT COUNT(s) > 0 FROM Settlement s WHERE s.user.id = :userId " +
            "AND s.status.name NOT IN ('COMPLETED', 'FAILED') " +
            "AND s.settlementDate <= :endDate AND s.completionDate >= :startDate")
    boolean hasActiveSettlementOverlap(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    long countByUserIdAndStatus_NameNotIn(Long userId, List<String> finishedStatuses);
}
