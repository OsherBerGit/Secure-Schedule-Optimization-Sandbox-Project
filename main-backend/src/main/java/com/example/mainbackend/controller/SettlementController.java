package com.example.mainbackend.controller;

import com.example.mainbackend.dto.settlement.SettlementCreateRequest;
import com.example.mainbackend.dto.settlement.SettlementResponseDto;
import com.example.mainbackend.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing settlements (final schedule assignments).
 * Provides endpoints for CRUD operations on worker-task assignments.
 */
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * Creates a new settlement (Manual Scheduling).
     * ALLOWED FOR: ADMIN, or MANAGER (only if both Task and Worker belong to their department).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @securityHelper.canManageTask(#request.taskId) and @securityHelper.canManageUser(#request.workerId))")
    public ResponseEntity<SettlementResponseDto> createSettlement(
            @Valid @RequestBody SettlementCreateRequest request) {
        SettlementResponseDto response = settlementService.createSettlement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a settlement by its ID.
     * ALLOWED FOR: ADMIN, MANAGER of the department, or the ASSIGNED WORKER.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityHelper.canViewSettlement(#id)")
    public ResponseEntity<SettlementResponseDto> getSettlementById(@PathVariable Long id) {
        SettlementResponseDto response = settlementService.getSettlementById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all settlements globally.
     * RESTRICTED TO ADMIN ONLY.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SettlementResponseDto>> getAllSettlements() {
        List<SettlementResponseDto> settlements = settlementService.getAllSettlements();
        return ResponseEntity.ok(settlements);
    }

    /**
     * Retrieves all settlements for a specific worker.
     * ALLOWED FOR: ADMIN, MANAGER of the worker, or the WORKER themselves.
     */
    @GetMapping("/worker/{workerId}")
    @PreAuthorize("@securityHelper.canManageUser(#workerId)")
    public ResponseEntity<List<SettlementResponseDto>> getSettlementsByWorker(@PathVariable Long workerId) {
        List<SettlementResponseDto> settlements = settlementService.getSettlementsByWorker(workerId);
        return ResponseEntity.ok(settlements);
    }

    /**
     * Returns all settlements for the currently authenticated user.
     * ALLOWED FOR: ANY AUTHENTICATED USER (Worker/Manager/Admin).
     */
    @GetMapping("/worker/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SettlementResponseDto>> getMySettlements(Authentication authentication) {
        String nationalId = authentication.getName();
        List<SettlementResponseDto> settlements = settlementService.getMySettlements(nationalId);
        return ResponseEntity.ok(settlements);
    }

    /**
     * Marks a settlement as COMPLETED.
     * ALLOWED FOR: The ASSIGNED WORKER (or Admin/Manager if you want to allow them).
     * Note: The service layer already verifies the nationalId matches the assigned worker.
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("@securityHelper.canViewSettlement(#id)")
    public ResponseEntity<SettlementResponseDto> completeSettlement(
            @PathVariable Long id, Authentication authentication) {
        String nationalId = authentication.getName();
        SettlementResponseDto response = settlementService.completeSettlement(id, nationalId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves settlements for a specific task.
     * ALLOWED FOR: ADMIN, MANAGER of the task, or ASSIGNED WORKER.
     */
    @GetMapping("/task/{taskId}")
    @PreAuthorize("@securityHelper.canViewTask(#taskId)")
    public ResponseEntity<List<SettlementResponseDto>> getSettlementsByTask(@PathVariable Long taskId) {
        List<SettlementResponseDto> settlements = settlementService.getSettlementsByTask(taskId);
        return ResponseEntity.ok(settlements);
    }

    /**
     * Retrieves settlements globally by date range.
     * RESTRICTED TO ADMIN ONLY.
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SettlementResponseDto>> getSettlementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<SettlementResponseDto> settlements = settlementService.getSettlementsByDateRange(startDate, endDate);
        return ResponseEntity.ok(settlements);
    }

    /**
     * Deletes a settlement.
     * ALLOWED FOR: ADMIN, or MANAGER of the department.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageSettlement(#id)")
    public ResponseEntity<Void> deleteSettlement(@PathVariable Long id) {
        settlementService.deleteSettlement(id);
        return ResponseEntity.noContent().build();
    }
}

