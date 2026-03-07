package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Returns only tasks with the given lifecycle status.
     * Used by SchedulingService — Zero-Trust filter (LOCKED/CLOSED tasks excluded).
     */
    @Query("SELECT t FROM Task t WHERE t.status.name = :statusName")
    List<Task> findByStatusName(@Param("statusName") String statusName);

    /**
     * Fetches OPEN tasks with their requiredRoles and incomingConstraints eagerly loaded
     * in a single DB round-trip to avoid N+1 queries in the scheduling engine.
     *
     * Two separate queries are required because JOIN FETCH on two collections
     * in a single query causes a Cartesian product (HHH90003004 warning).
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.requiredRoles " +
           "WHERE t.status.name = :statusName")
    List<Task> findOpenTasksWithRoles(@Param("statusName") String statusName);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.incomingConstraints ic " +
           "LEFT JOIN FETCH ic.predecessorTask " +
           "WHERE t.status.name = :statusName")
    List<Task> findOpenTasksWithConstraints(@Param("statusName") String statusName);
}
