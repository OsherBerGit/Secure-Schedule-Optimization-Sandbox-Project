package com.example.mainbackend.algorithm.controller;

import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import com.example.mainbackend.algorithm.dto.SaveScheduleRequest;
import com.example.mainbackend.algorithm.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for the two-phase scheduling flow:
 *
 * <ol>
 *   <li>{@code POST /api/schedule/run}  — generates a draft preview (no DB writes)</li>
 *   <li>{@code POST /api/schedule/save} — persists the admin-approved draft to the DB</li>
 * </ol>
 *
 * Only ADMIN can trigger either endpoint.
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    /**
     * PHASE 1 — Generate a draft schedule preview.
     *
     * <p>Runs the algorithm and returns enriched assignments for the admin to review.
     * <strong>Does not write anything to the database.</strong></p>
     *
     * @param strategy     "GREEDY" (default) | "ROUND_ROBIN" | "MEMETIC" | "Constraint Programming"
     * @param departmentId optional — ADMIN only: scope the run to a specific department.
     *                     Ignored for MANAGER (always scoped to their own department).
     *                     When null for ADMIN, all departments are included.
     */
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlgoScheduleResponse> runSchedule(
            @RequestParam(defaultValue = "GREEDY") String strategy,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long configId,
            Authentication authentication) {

        String nationalId = authentication.getName();

        AlgoScheduleResponse response = schedulingService.runScheduling(strategy, departmentId, configId, nationalId);
        return ResponseEntity.ok(response);
    }

    /**
     * PHASE 2 — Approve and persist the draft schedule.
     *
     * <p>Accepts the assignments the admin approved in the frontend and writes them
     * to the database: Task → SCHEDULED, Settlement → ASSIGNED.</p>
     */
    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> saveSchedule(@Valid @RequestBody SaveScheduleRequest request,
                                             Authentication authentication) {

        String nationalId = authentication.getName();

        schedulingService.saveApprovedSchedule(request, nationalId);
        return ResponseEntity.ok().build();
    }
}
