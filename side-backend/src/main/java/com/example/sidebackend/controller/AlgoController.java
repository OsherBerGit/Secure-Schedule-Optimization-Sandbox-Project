package com.example.sidebackend.controller;

import com.example.sidebackend.dto.SchedulingRequestDto;
import com.example.sidebackend.dto.SchedulingResponseDto;
import com.example.sidebackend.service.AlgoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AlgoController — exposes the scheduling endpoint to the main-backend.
 *
 * <p>Single responsibility: accept the HTTP request, delegate all business
 * logic to {@link AlgoService}, and return the HTTP response.</p>
 *
 * <p>Base path: {@code /api/v1/algo}</p>
 */
@RestController
@RequestMapping("/api/v1/algo")
public class AlgoController {

    private static final Logger log = LoggerFactory.getLogger(AlgoController.class);

    private final AlgoService algoService;

    public AlgoController(AlgoService algoService) {
        this.algoService = algoService;
    }

    /**
     * Runs the scheduling algorithm for the supplied workers and tasks.
     *
     * <p>The request body is validated via {@code @Valid} before the service
     * layer is reached. Any constraint violations are returned as a 400 response
     * by Spring's default {@code MethodArgumentNotValidException} handler.</p>
     *
     * @param request the scheduling request (workers + tasks + optional strategy)
     * @return 200 OK with the scheduling result
     */
    @PostMapping("/schedule")
    public ResponseEntity<SchedulingResponseDto> schedule(
            @Valid @RequestBody SchedulingRequestDto request) {

        log.info("[AlgoController] POST /api/v1/algo/schedule — strategy='{}', users={}, tasks={}",
                request.strategy(), request.users().size(), request.tasks().size());

        SchedulingResponseDto response = algoService.schedule(request);

        log.info("[AlgoController] Scheduling complete — assigned={}/{}, strategy='{}'",
                response.assignedTasks(), response.totalTasks(), response.strategyUsed());

        return ResponseEntity.ok(response);
    }
}

