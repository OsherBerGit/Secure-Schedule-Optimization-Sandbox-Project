package com.example.mainbackend.controller;

import com.example.mainbackend.dto.vacation.VacationCreateRequest;
import com.example.mainbackend.dto.vacation.VacationRequestDto;
import com.example.mainbackend.dto.vacation.VacationResponseDto;
import com.example.mainbackend.dto.vacation.VacationStatusUpdateRequest;
import com.example.mainbackend.service.VacationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vacations")
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @securityHelper.canManageUser(#request.workerId))")
    public ResponseEntity<VacationResponseDto> createVacation(
            @Valid @RequestBody VacationCreateRequest request) {
        VacationResponseDto response = vacationService.createVacation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('WORKER', 'MANAGER')")
    public ResponseEntity<VacationResponseDto> requestVacation(
            @Valid @RequestBody VacationRequestDto request) {
        VacationResponseDto response = vacationService.requestVacation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@securityHelper.canManageVacation(#id)")
    public ResponseEntity<VacationResponseDto> updateVacationStatus(
            @PathVariable Long id,
            @Valid @RequestBody VacationStatusUpdateRequest request) {
        VacationResponseDto response = vacationService.updateVacationStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityHelper.canViewVacation(#id)")
    public ResponseEntity<VacationResponseDto> getVacationById(@PathVariable Long id) {
        VacationResponseDto response = vacationService.getVacationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VacationResponseDto>> getAllVacations() {
        List<VacationResponseDto> vacations = vacationService.getAllVacations();
        return ResponseEntity.ok(vacations);
    }

    @GetMapping("/worker/{workerId}")
    @PreAuthorize("@securityHelper.canManageUser(#workerId)")
    public ResponseEntity<List<VacationResponseDto>> getVacationsByWorker(@PathVariable Long workerId) {
        List<VacationResponseDto> vacations = vacationService.getVacationsByWorker(workerId);
        return ResponseEntity.ok(vacations);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VacationResponseDto>> getVacationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<VacationResponseDto> vacations = vacationService.getVacationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(vacations);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageVacation(#id)")
    public ResponseEntity<VacationResponseDto> updateVacation(
            @PathVariable Long id,
            @Valid @RequestBody VacationCreateRequest request) {
        VacationResponseDto response = vacationService.updateVacation(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageVacation(#id)")
    public ResponseEntity<Void> deleteVacation(@PathVariable Long id) {
        vacationService.deleteVacation(id);
        return ResponseEntity.noContent().build();
    }
}
