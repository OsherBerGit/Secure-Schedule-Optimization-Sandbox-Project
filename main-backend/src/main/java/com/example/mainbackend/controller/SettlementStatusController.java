package com.example.mainbackend.controller;

import com.example.mainbackend.entity.SettlementStatus;
import com.example.mainbackend.repository.SettlementStatusRepository;
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

    private final SettlementStatusRepository settlementStatusRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SettlementStatus>> getAll() {
        return ResponseEntity.ok(settlementStatusRepository.findAll());
    }
}

