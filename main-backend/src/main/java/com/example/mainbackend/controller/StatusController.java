package com.example.mainbackend.controller;

import com.example.mainbackend.dto.status.StatusRequest;
import com.example.mainbackend.dto.status.StatusResponseDto;
import com.example.mainbackend.service.StatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing task statuses.
 * Provides endpoints for CRUD operations on status lookup table.
 */
@RestController
@RequestMapping("/api/statuses")
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatusResponseDto> createStatus(@Valid @RequestBody StatusRequest request) {
        StatusResponseDto response = statusService.createStatus(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<StatusResponseDto> getStatusById(@PathVariable Long id) {
        StatusResponseDto response = statusService.getStatusById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<StatusResponseDto>> getAllStatuses() {
        List<StatusResponseDto> statuses = statusService.getAllStatuses();
        return ResponseEntity.ok(statuses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatusResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        StatusResponseDto response = statusService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id) {
        statusService.deleteStatus(id);
        return ResponseEntity.noContent().build();
    }
}

