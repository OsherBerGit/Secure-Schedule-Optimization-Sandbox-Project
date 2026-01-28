package com.example.mainbackend.repository;

import com.example.mainbackend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedEmployeeId(Long workerId);
    List<Task> findByStatusName(String statusName);
}
