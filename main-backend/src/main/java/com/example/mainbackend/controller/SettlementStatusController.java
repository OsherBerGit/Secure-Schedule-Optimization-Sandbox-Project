package com.example.mainbackend.controller;

import com.example.mainbackend.dto.settlementstatus.SettlementStatusResponseDto;
import com.example.mainbackend.service.SettlementStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only endpoint for Settlement Statuses (PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, FAILED).
 * These are seeded by DataLoader and managed by the system — no create/update/delete exposed.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlement-statuses")
public class SettlementStatusController {

    private final SettlementStatusService settlementStatusService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SettlementStatusResponseDto>> getAllStatuses() {
        return ResponseEntity.ok(settlementStatusService.getAllStatuses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SettlementStatusResponseDto> getStatusById(@PathVariable Long id) {
        return ResponseEntity.ok(settlementStatusService.getStatusById(id));
    }
}

