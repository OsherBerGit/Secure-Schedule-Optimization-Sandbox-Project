package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    /**
     * Returns only OPEN tasks — used by SchedulingService (Zero-Trust filter).
     * No category check needed: task_statuses table only holds task lifecycle statuses.
     */
    @Query("SELECT t FROM Task t WHERE t.status.name = :statusName")
    List<Task> findByStatusName(String statusName);
}
