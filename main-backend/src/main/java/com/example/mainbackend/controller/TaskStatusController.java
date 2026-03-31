package com.example.mainbackend.controller;

import com.example.mainbackend.dto.taskstatus.TaskStatusResponseDto;
import com.example.mainbackend.service.TaskStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing task statuses.
 * Provides endpoints for CRUD operations on status lookup table.
 */
@RestController
@RequestMapping("/api/task-statuses")
@RequiredArgsConstructor
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskStatusResponseDto>> getAllStatuses() {
        return ResponseEntity.ok(taskStatusService.getAllStatuses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskStatusResponseDto> getStatusById(@PathVariable Long id) {
        return ResponseEntity.ok(taskStatusService.getStatusById(id));
    }
}

