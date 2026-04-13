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

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (#departmentId == null or @securityHelper.canManageDepartment(#departmentId))")
    public ResponseEntity<AlgoScheduleResponse> runSchedule(
            @RequestParam(defaultValue = "GREEDY") String strategy,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long configId,
            Authentication authentication) {

        String nationalId = authentication.getName();
        AlgoScheduleResponse response = schedulingService.runScheduling(strategy, departmentId, configId, nationalId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> saveSchedule(@Valid @RequestBody SaveScheduleRequest request,
                                             Authentication authentication) {

        String nationalId = authentication.getName();
        schedulingService.saveApprovedSchedule(request, nationalId);
        return ResponseEntity.ok().build();
    }
}
