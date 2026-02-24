package com.example.mainbackend.controller;

import com.example.mainbackend.dto.priority.PriorityRequest;
import com.example.mainbackend.dto.priority.PriorityResponseDto;
import com.example.mainbackend.service.PriorityService;
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
public class PriorityController {

    private final PriorityService priorityService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PriorityResponseDto> createPriority(@Valid @RequestBody PriorityRequest request) {
        PriorityResponseDto response = priorityService.createPriority(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<PriorityResponseDto> getPriorityById(@PathVariable Long id) {
        PriorityResponseDto response = priorityService.getPriorityById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<PriorityResponseDto>> getAllPriorities() {
        List<PriorityResponseDto> priorities = priorityService.getAllPriorities();
        return ResponseEntity.ok(priorities);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PriorityResponseDto> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody PriorityRequest request) {
        PriorityResponseDto response = priorityService.updatePriority(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePriority(@PathVariable Long id) {
        priorityService.deletePriority(id);
        return ResponseEntity.noContent().build();
    }
}

