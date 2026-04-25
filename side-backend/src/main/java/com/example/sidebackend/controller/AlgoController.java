package com.example.sidebackend.controller;

import com.example.sidebackend.dto.SchedulingRequestDto;
import com.example.sidebackend.dto.SchedulingResponseDto;
import com.example.sidebackend.service.AlgoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/algo")
public class AlgoController {

    private static final Logger log = LoggerFactory.getLogger(AlgoController.class);

    private final AlgoService algoService;

    public AlgoController(AlgoService algoService) {
        this.algoService = algoService;
    }

    @PostMapping("/schedule")
    public ResponseEntity<SchedulingResponseDto> schedule(
            @Valid @RequestBody SchedulingRequestDto request) {

        log.info("[AlgoController] POST /api/v1/algo/schedule — strategy='{}', users={}, tasks={}",
                request.strategy(), request.users().size(), request.tasks().size());

        SchedulingResponseDto response = algoService.schedule(request);

        log.info("[AlgoController] Scheduling complete — assigned={}/{}, strategy='{}'",
                response.getAssignedTasks(), response.getAssignedTasks(), response.getStrategyUsed());

        return ResponseEntity.ok(response);
    }
}

