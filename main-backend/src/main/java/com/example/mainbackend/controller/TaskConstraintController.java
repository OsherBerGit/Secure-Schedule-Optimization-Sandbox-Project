package com.example.mainbackend.controller;

import com.example.mainbackend.dto.taskconstraint.TaskConstraintCreateRequest;
import com.example.mainbackend.dto.taskconstraint.TaskConstraintResponseDto;
import com.example.mainbackend.service.TaskConstraintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-constraints")
@RequiredArgsConstructor
@Slf4j
public class TaskConstraintController {

    private final TaskConstraintService taskConstraintService;

    /**
     * Creates a new constraint between two tasks.
     * ALLOWED FOR: ADMIN, or MANAGER (if they manage BOTH tasks involved).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @securityHelper.canManageTask(#request.predecessorTaskId) and @securityHelper.canManageTask(#request.successorTaskId))")
    public ResponseEntity<TaskConstraintResponseDto> createConstraint(
            @Valid @RequestBody TaskConstraintCreateRequest request) {
        TaskConstraintResponseDto response = taskConstraintService.createConstraint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a constraint by ID.
     * ALLOWED FOR: ADMIN, MANAGER of the tasks, or WORKER assigned to either task.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityHelper.canViewConstraint(#id)")
    public ResponseEntity<TaskConstraintResponseDto> getConstraintById(@PathVariable Long id) {
        TaskConstraintResponseDto response = taskConstraintService.getConstraintById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all constraints globally.
     * RESTRICTED TO ADMIN ONLY.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskConstraintResponseDto>> getAllConstraints() {
        List<TaskConstraintResponseDto> constraints = taskConstraintService.getAllConstraints();
        return ResponseEntity.ok(constraints);
    }

    /**
     * Retrieves constraints where the given task is the predecessor.
     * ALLOWED FOR: Anyone who can view the predecessor task.
     */
    @GetMapping("/predecessor/{taskId}")
    @PreAuthorize("@securityHelper.canViewTask(#taskId)")
    public ResponseEntity<List<TaskConstraintResponseDto>> getConstraintsByPredecessor(
            @PathVariable Long taskId) {
        List<TaskConstraintResponseDto> constraints = taskConstraintService.getConstraintsByPredecessorTask(taskId);
        return ResponseEntity.ok(constraints);
    }

    /**
     * Retrieves constraints where the given task is the successor.
     * ALLOWED FOR: Anyone who can view the successor task.
     */
    @GetMapping("/successor/{taskId}")
    @PreAuthorize("@securityHelper.canViewTask(#taskId)")
    public ResponseEntity<List<TaskConstraintResponseDto>> getConstraintsBySuccessor(
            @PathVariable Long taskId) {
        List<TaskConstraintResponseDto> constraints = taskConstraintService.getConstraintsBySuccessorTask(taskId);
        return ResponseEntity.ok(constraints);
    }

    /**
     * Updates an existing constraint.
     * ALLOWED FOR: ADMIN, or MANAGER of the tasks.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageConstraint(#id)")
    public ResponseEntity<TaskConstraintResponseDto> updateConstraint(
            @PathVariable Long id,
            @Valid @RequestBody TaskConstraintCreateRequest request) {
        TaskConstraintResponseDto response = taskConstraintService.updateConstraint(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a constraint.
     * ALLOWED FOR: ADMIN, or MANAGER of the tasks.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageConstraint(#id)")
    public ResponseEntity<Void> deleteConstraint(@PathVariable Long id) {
        taskConstraintService.deleteConstraint(id);
        return ResponseEntity.noContent().build();
    }
}

