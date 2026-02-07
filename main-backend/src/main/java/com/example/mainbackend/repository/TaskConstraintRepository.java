package com.example.mainbackend.repository;

import com.example.mainbackend.entity.TaskConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskConstraintRepository extends JpaRepository<TaskConstraint, Long> {

    /**
     * Find all constraints where the given task is the predecessor.
     * Useful for finding what tasks depend on this one.
     */
    List<TaskConstraint> findByPredecessorTaskId(Long taskId);

    /**
     * Find all constraints where the given task is the successor.
     * Useful for finding what tasks this one depends on.
     */
    List<TaskConstraint> findBySuccessorTaskId(Long taskId);

    /**
     * Find all constraints of a specific type.
     */
    List<TaskConstraint> findByConstraintTypeId(Long constraintTypeId);

    /**
     * Batch load all constraints with their related tasks - optimized for algorithm server.
     * Uses JOIN FETCH to avoid N+1 query problem.
     */
    @Query("SELECT tc FROM TaskConstraint tc " +
           "JOIN FETCH tc.predecessorTask " +
           "JOIN FETCH tc.successorTask " +
           "JOIN FETCH tc.constraintType")
    List<TaskConstraint> findAllWithTasks();

    /**
     * Check if a constraint already exists between two tasks.
     * Prevents duplicate constraints.
     */
    boolean existsByPredecessorTaskIdAndSuccessorTaskId(Long predecessorId, Long successorId);
}
