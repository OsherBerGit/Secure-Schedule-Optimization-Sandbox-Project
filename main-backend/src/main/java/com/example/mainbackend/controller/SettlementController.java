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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettlementResponseDto> createSettlement(
            @Valid @RequestBody SettlementCreateRequest request) {
        SettlementResponseDto response = settlementService.createSettlement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<SettlementResponseDto> getSettlementById(@PathVariable Long id) {
        SettlementResponseDto response = settlementService.getSettlementById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<SettlementResponseDto>> getAllSettlements() {
        List<SettlementResponseDto> settlements = settlementService.getAllSettlements();
        return ResponseEntity.ok(settlements);
    }

    @GetMapping("/worker/{workerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<SettlementResponseDto>> getSettlementsByWorker(@PathVariable Long workerId) {
        List<SettlementResponseDto> settlements = settlementService.getSettlementsByWorker(workerId);
        return ResponseEntity.ok(settlements);
    }

    /**
     * GET /api/settlements/worker/me
     * Returns all settlements for the currently authenticated worker.
     * Extracts nationalId from the JWT via SecurityContextHolder.
     */
    @GetMapping("/worker/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<SettlementResponseDto>> getMySettlements(Authentication authentication) {
        String nationalId = authentication.getName();
        List<SettlementResponseDto> settlements = settlementService.getMySettlements(nationalId);
        return ResponseEntity.ok(settlements);
    }

    /**
     * PATCH /api/settlements/{id}/complete
     * Marks a settlement as COMPLETED. Only the owning worker may call this.
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<SettlementResponseDto> completeSettlement(
            @PathVariable Long id, Authentication authentication) {
        String nationalId = authentication.getName();
        SettlementResponseDto response = settlementService.completeSettlement(id, nationalId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<SettlementResponseDto>> getSettlementsByTask(@PathVariable Long taskId) {
        List<SettlementResponseDto> settlements = settlementService.getSettlementsByTask(taskId);
        return ResponseEntity.ok(settlements);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<SettlementResponseDto>> getSettlementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<SettlementResponseDto> settlements = settlementService.getSettlementsByDateRange(startDate, endDate);
        return ResponseEntity.ok(settlements);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSettlement(@PathVariable Long id) {
        settlementService.deleteSettlement(id);
        return ResponseEntity.noContent().build();
    }
}

