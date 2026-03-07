package com.example.mainbackend.algorithm.controller;

import com.example.mainbackend.algorithm.service.SchedulingService;
import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint that triggers the scheduling algorithm.
 *
 * POST /api/schedule/run?strategy=GREEDY
 * POST /api/schedule/run?strategy=ROUND_ROBIN
 *
 * Only ADMIN can trigger scheduling.
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    /**
     * Triggers the scheduling algorithm, persists results to DB, and returns the assignments.
     *
     * @param strategy "GREEDY" (default) or "ROUND_ROBIN"
     */
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlgoScheduleResponse> runSchedule(
            @RequestParam(defaultValue = "GREEDY") String strategy) {
        AlgoScheduleResponse response = schedulingService.runScheduling(strategy);
        return ResponseEntity.ok(response);
    }
}

