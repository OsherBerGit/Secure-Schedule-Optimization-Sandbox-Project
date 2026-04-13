package com.example.mainbackend.service;

import com.example.mainbackend.dto.taskconstraint.TaskConstraintCreateRequest;
import com.example.mainbackend.dto.taskconstraint.TaskConstraintResponseDto;
import com.example.mainbackend.entity.ConstraintType;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskConstraint;
import com.example.mainbackend.mapper.TaskConstraintMapper;
import com.example.mainbackend.repository.ConstraintTypeRepository;
import com.example.mainbackend.repository.TaskConstraintRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.security.SecurityHelper;
import com.example.mainbackend.util.CycleDetectionUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskConstraintService {

    private final TaskConstraintRepository taskConstraintRepository;
    private final TaskRepository taskRepository;
    private final ConstraintTypeRepository constraintTypeRepository;
    private final TaskConstraintMapper mapper;
    private final SecurityHelper securityHelper;
    private final CycleDetectionUtil cycleDetectionUtil;

    @Transactional
    public TaskConstraintResponseDto createConstraint(TaskConstraintCreateRequest request) {
        if (request.getPredecessorTaskId().equals(request.getSuccessorTaskId()))
            throw new IllegalArgumentException("A task cannot have a constraint with itself");

        if (taskConstraintRepository.existsByPredecessorTaskIdAndSuccessorTaskId(request.getPredecessorTaskId(), request.getSuccessorTaskId()))
            throw new IllegalArgumentException("Constraint already exists between these tasks");

        // Fetch entities
        Task predecessorTask = taskRepository.findById(request.getPredecessorTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Predecessor task not found with ID: " + request.getPredecessorTaskId()));

        Task successorTask = taskRepository.findById(request.getSuccessorTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Successor task not found with ID: " + request.getSuccessorTaskId()));

        ConstraintType constraintType = constraintTypeRepository.findById(request.getConstraintTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Constraint type not found with ID: " + request.getConstraintTypeId()));

        // Validate for circular dependency BEFORE saving
        if (hasCircularDependency(request.getPredecessorTaskId(), request.getSuccessorTaskId()))
            throw new IllegalArgumentException("Adding this constraint would create a circular dependency in the task graph");

        TaskConstraint constraint = TaskConstraint.builder()
                .predecessorTask(predecessorTask)
                .successorTask(successorTask)
                .constraintType(constraintType)
                .lagMinutes(request.getLagMinutes() != null ? request.getLagMinutes() : 0)
                .build();

        TaskConstraint saved = taskConstraintRepository.save(constraint);
        return mapper.toDto(saved);
    }

    @Transactional
    public TaskConstraintResponseDto getConstraintById(Long id) {
        TaskConstraint constraint = taskConstraintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task constraint not found with ID: " + id));
        return mapper.toDto(constraint);
    }

    @Transactional
    public List<TaskConstraintResponseDto> getAllConstraints() {
        if (securityHelper.isManager()) {
            Long deptId = securityHelper.getCurrentUserDepartmentId();
            return taskConstraintRepository.findAll().stream()
                    .filter(c -> c.getPredecessorTask().getDepartment() != null
                              && c.getPredecessorTask().getDepartment().getId().equals(deptId))
                    .map(mapper::toDto)
                    .toList();
        }
        return taskConstraintRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public List<TaskConstraintResponseDto> getConstraintsByPredecessorTask(Long taskId) {
        return taskConstraintRepository.findByPredecessorTaskId(taskId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public List<TaskConstraintResponseDto> getConstraintsBySuccessorTask(Long taskId) {
        return taskConstraintRepository.findBySuccessorTaskId(taskId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public TaskConstraintResponseDto updateConstraint(Long id, TaskConstraintCreateRequest request) {
        TaskConstraint existing = taskConstraintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task constraint not found with ID: " + id));

        // Validate self-reference
        if (request.getPredecessorTaskId().equals(request.getSuccessorTaskId()))
            throw new IllegalArgumentException("A task cannot have a constraint with itself");

        // If tasks are changing, validate circular dependency
        if (!existing.getPredecessorTask().getId().equals(request.getPredecessorTaskId()) ||
            !existing.getSuccessorTask().getId().equals(request.getSuccessorTaskId())) {

            // Temporarily remove this constraint from the DB and flush the transaction.
            // WHY: hasCircularDependency() rebuilds the graph from taskConstraintRepository.findAll().
            // If we don't flush the deletion to the database first, the old edge will still be 
            // queried, which might cause a false-positive cycle detection.
            taskConstraintRepository.delete(existing);
            taskConstraintRepository.flush();

            if (hasCircularDependency(request.getPredecessorTaskId(), request.getSuccessorTaskId()))
                throw new IllegalArgumentException("Adding this constraint would create a circular dependency in the task graph");

            // Re-fetch entities
            Task predecessorTask = taskRepository.findById(request.getPredecessorTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Predecessor task not found"));
            Task successorTask = taskRepository.findById(request.getSuccessorTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Successor task not found"));

            existing.setPredecessorTask(predecessorTask);
            existing.setSuccessorTask(successorTask);
        }

        // Update constraint type
        ConstraintType constraintType = constraintTypeRepository.findById(request.getConstraintTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Constraint type not found"));
        existing.setConstraintType(constraintType);

        // Update lag minutes
        existing.setLagMinutes(request.getLagMinutes() != null ? request.getLagMinutes() : 0);

        TaskConstraint updated = taskConstraintRepository.save(existing);

        return mapper.toDto(updated);
    }

    @Transactional
    public boolean deleteConstraint(Long id) {
        if (!taskConstraintRepository.existsById(id))
            throw new IllegalArgumentException("Task constraint not found with ID: " + id);

        taskConstraintRepository.deleteById(id);
        return true;
    }

    private boolean hasCircularDependency(Long predecessorId, Long successorId) {
        List<TaskConstraint> allConstraints = taskConstraintRepository.findAll();
        Map<Long, List<Long>> graph = cycleDetectionUtil.buildGraph(allConstraints);
        return cycleDetectionUtil.wouldCreateCycle(predecessorId, successorId, graph);
    }
}
