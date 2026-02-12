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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskConstraintResponseDto> createConstraint(
            @Valid @RequestBody TaskConstraintCreateRequest request) {
        TaskConstraintResponseDto response = taskConstraintService.createConstraint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<TaskConstraintResponseDto> getConstraintById(@PathVariable Long id) {
        TaskConstraintResponseDto response = taskConstraintService.getConstraintById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<TaskConstraintResponseDto>> getAllConstraints() {
        List<TaskConstraintResponseDto> constraints = taskConstraintService.getAllConstraints();
        return ResponseEntity.ok(constraints);
    }

    @GetMapping("/predecessor/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<TaskConstraintResponseDto>> getConstraintsByPredecessor(
            @PathVariable Long taskId) {
        List<TaskConstraintResponseDto> constraints = taskConstraintService.getConstraintsByPredecessorTask(taskId);
        return ResponseEntity.ok(constraints);
    }

    @GetMapping("/successor/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<TaskConstraintResponseDto>> getConstraintsBySuccessor(
            @PathVariable Long taskId) {
        List<TaskConstraintResponseDto> constraints = taskConstraintService.getConstraintsBySuccessorTask(taskId);
        return ResponseEntity.ok(constraints);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskConstraintResponseDto> updateConstraint(
            @PathVariable Long id,
            @Valid @RequestBody TaskConstraintCreateRequest request) {
        TaskConstraintResponseDto response = taskConstraintService.updateConstraint(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConstraint(@PathVariable Long id) {
        taskConstraintService.deleteConstraint(id);
        return ResponseEntity.noContent().build();
    }
}

