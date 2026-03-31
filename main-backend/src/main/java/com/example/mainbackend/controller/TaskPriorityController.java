package com.example.mainbackend.controller;

import com.example.mainbackend.dto.taskpriority.TaskPriorityRequest;
import com.example.mainbackend.dto.taskpriority.TaskPriorityResponseDto;
import com.example.mainbackend.service.TaskPriorityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing task priorities.
 * Provides endpoints for CRUD operations on priority lookup table.
 */
@RestController
@RequestMapping("/api/priorities")
@RequiredArgsConstructor
public class TaskPriorityController {

    private final TaskPriorityService taskPriorityService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskPriorityResponseDto>> getAllPriorities() {
        return ResponseEntity.ok(taskPriorityService.getAllPriorities());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskPriorityResponseDto> getPriorityById(@PathVariable Long id) {
        return ResponseEntity.ok(taskPriorityService.getPriorityById(id));
    }
}

