package com.example.mainbackend.repository;

import com.example.mainbackend.entity.TaskConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskConstraintRepository extends JpaRepository<TaskConstraint, Long> {
    List<TaskConstraint> findByFirstTaskId(Long taskId);
    List<TaskConstraint> findBySecondTaskId(Long taskId);
    List<TaskConstraint> findByConstraintTypeId(Long constraintTypeId);
}
