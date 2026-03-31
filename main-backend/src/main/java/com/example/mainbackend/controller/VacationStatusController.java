package com.example.mainbackend.controller;

import com.example.mainbackend.dto.vacationstatus.VacationStatusResponseDto;
import com.example.mainbackend.service.VacationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vacation-statuses")
@RequiredArgsConstructor
public class VacationStatusController {

    private final VacationStatusService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VacationStatusResponseDto>> getAll() {
        return ResponseEntity.ok(service.getAllStatuses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VacationStatusResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getStatusById(id));
    }
}
