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

    /** All tasks scoped to a specific department (any status). */
    List<Task> findAllByDepartmentId(Long departmentId);

    // ── ADMIN scope (all departments) ────────────────────────────────────────

    /**
     * Fetches ALL OPEN tasks with their requiredRoles eagerly loaded.
     * Used by SchedulingService (ADMIN scope).
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.requiredRoles " +
           "WHERE t.status.name = :statusName")
    List<Task> findOpenTasksWithRoles(@Param("statusName") String statusName);

    /**
     * Fetches ALL OPEN tasks with their incomingConstraints eagerly loaded.
     * Used by SchedulingService (ADMIN scope).
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.incomingConstraints ic " +
           "LEFT JOIN FETCH ic.predecessorTask " +
           "WHERE t.status.name = :statusName")
    List<Task> findOpenTasksWithConstraints(@Param("statusName") String statusName);

    // ── MANAGER scope (single department) ────────────────────────────────────

    /**
     * Fetches OPEN tasks for a specific department with their requiredRoles eagerly loaded.
     * Used by SchedulingService (MANAGER scope).
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.requiredRoles " +
           "WHERE t.status.name = :statusName AND t.department.id = :departmentId")
    List<Task> findOpenTasksWithRolesByDepartment(@Param("statusName") String statusName,
                                                  @Param("departmentId") Long departmentId);

    /**
     * Fetches OPEN tasks for a specific department with their incomingConstraints eagerly loaded.
     * Used by SchedulingService (MANAGER scope).
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.incomingConstraints ic " +
           "LEFT JOIN FETCH ic.predecessorTask " +
           "WHERE t.status.name = :statusName AND t.department.id = :departmentId")
    List<Task> findOpenTasksWithConstraintsByDepartment(@Param("statusName") String statusName,
                                                        @Param("departmentId") Long departmentId);
}
