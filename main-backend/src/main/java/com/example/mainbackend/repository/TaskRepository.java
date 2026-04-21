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
     * Used by SchedulingService Zero-Trust filter (LOCKED/CLOSED tasks excluded).
     */
    @Query("SELECT t FROM Task t WHERE t.status.name = :statusName")
    List<Task> findByStatusName(@Param("statusName") String statusName);

    List<Task> findAllByDepartmentId(Long departmentId);

    List<Task> findByStatusId(Long statusId);

    // ADMIN scope (all departments)

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.requiredSkills " +
           "WHERE t.status.name = :statusName")
    List<Task> findOpenTasksWithSkills(@Param("statusName") String statusName);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.incomingConstraints ic " +
           "LEFT JOIN FETCH ic.predecessorTask " +
           "WHERE t.status.name = :statusName")
    List<Task> findOpenTasksWithConstraints(@Param("statusName") String statusName);

    // MANAGER scope (single department)

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.requiredSkills " +
           "WHERE t.status.name = :statusName AND t.department.id = :departmentId")
    List<Task> findOpenTasksWithSkillsByDepartment(@Param("statusName") String statusName,
                                                   @Param("departmentId") Long departmentId);

   @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.incomingConstraints ic " +
           "LEFT JOIN FETCH ic.predecessorTask " +
           "WHERE t.status.name = :statusName AND t.department.id = :departmentId")
    List<Task> findOpenTasksWithConstraintsByDepartment(@Param("statusName") String statusName,
                                                        @Param("departmentId") Long departmentId);
}
