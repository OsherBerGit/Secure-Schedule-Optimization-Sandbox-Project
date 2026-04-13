package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    boolean existsByTask_IdAndWorker_Id(Long taskId, Long userId);

    // Eagerly load all associations to prevent LazyInitializationException outside transactions.
    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.worker JOIN FETCH s.status")
    List<Settlement> findAllWithDetails();

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.worker JOIN FETCH s.status WHERE s.id = :id")
    Optional<Settlement> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.worker JOIN FETCH s.status WHERE s.worker.id = :workerId")
    List<Settlement> findByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.worker JOIN FETCH s.status WHERE s.task.id = :taskId")
    List<Settlement> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.worker JOIN FETCH s.status WHERE s.task.id IN :taskIds")
    List<Settlement> findByTaskIdIn(@Param("taskIds") java.util.Collection<Long> taskIds);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.worker JOIN FETCH s.status WHERE s.settlementDate BETWEEN :start AND :end")
    List<Settlement> findBySettlementDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.task JOIN FETCH s.worker JOIN FETCH s.status WHERE s.worker.nationalId = :nationalId")
    List<Settlement> findByWorker_NationalId(@Param("nationalId") String nationalId);

    long countByWorker_IdAndStatus_NameIn(Long workerId, java.util.Collection<String> statusNames);

    @Query("SELECT s.worker.id, COUNT(s) FROM Settlement s WHERE s.worker.id IN :workerIds AND s.status.name IN :statusNames GROUP BY s.worker.id")
    List<Object[]> countActiveSettlementsByWorkerIds(@Param("workerIds") java.util.Collection<Long> workerIds, @Param("statusNames") java.util.Collection<String> statusNames);
}
