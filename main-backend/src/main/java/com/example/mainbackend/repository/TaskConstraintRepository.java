package com.example.mainbackend.repository;

import com.example.mainbackend.entity.TaskConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskConstraintRepository extends JpaRepository<TaskConstraint, Long> {

    List<TaskConstraint> findByPredecessorTaskId(Long taskId);

    List<TaskConstraint> findBySuccessorTaskId(Long taskId);

    /**
     * Batch load all constraints with their related tasks - optimized for algorithm server.
     * Uses JOIN FETCH to avoid N+1 query problem.
     */
    @Query("SELECT tc FROM TaskConstraint tc " +
           "JOIN FETCH tc.predecessorTask " +
           "JOIN FETCH tc.successorTask " +
           "JOIN FETCH tc.constraintType")
    List<TaskConstraint> findAllWithTasks();

    boolean existsByPredecessorTaskIdAndSuccessorTaskId(Long predecessorId, Long successorId);
}
