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

    /**
     * Creates a vacation directly (auto-approved).
     * ALLOWED FOR: ADMIN, or MANAGER (if they are creating it for a worker in their department).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @securityHelper.canManageUser(#request.workerId))")
    public ResponseEntity<VacationResponseDto> createVacation(
            @Valid @RequestBody VacationCreateRequest request) {
        VacationResponseDto response = vacationService.createVacation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submits a vacation request (starts as PENDING).
     * ALLOWED FOR: WORKER or MANAGER (Managers can also request vacations for themselves).
     */
    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('WORKER', 'MANAGER')")
    public ResponseEntity<VacationResponseDto> requestVacation(
            @Valid @RequestBody VacationRequestDto request) {
        VacationResponseDto response = vacationService.requestVacation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Approves or rejects a PENDING vacation request.
     * ALLOWED FOR: ADMIN, or MANAGER of the vacation's department.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("@securityHelper.canManageVacation(#id)")
    public ResponseEntity<VacationResponseDto> updateVacationStatus(
            @PathVariable Long id,
            @Valid @RequestBody VacationStatusUpdateRequest request) {
        VacationResponseDto response = vacationService.updateVacationStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific vacation by its ID.
     * ALLOWED FOR: ADMIN, MANAGER of the department, or the WORKER who owns it.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityHelper.canViewVacation(#id)")
    public ResponseEntity<VacationResponseDto> getVacationById(@PathVariable Long id) {
        VacationResponseDto response = vacationService.getVacationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all vacations globally.
     * ALLOWED FOR: Any authenticated user (service will filter by department if manager).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VacationResponseDto>> getAllVacations() {
        List<VacationResponseDto> vacations = vacationService.getAllVacations();
        return ResponseEntity.ok(vacations);
    }

    /**
     * Retrieves vacations by a specific worker.
     * ALLOWED FOR: ADMIN, MANAGER of the worker, or the WORKER themselves.
     */
    @GetMapping("/worker/{workerId}")
    @PreAuthorize("@securityHelper.canManageUser(#workerId)")
    public ResponseEntity<List<VacationResponseDto>> getVacationsByWorker(@PathVariable Long workerId) {
        List<VacationResponseDto> vacations = vacationService.getVacationsByWorker(workerId);
        return ResponseEntity.ok(vacations);
    }

    /**
     * Retrieves all vacations within a date range (Global Search).
     * RESTRICTED TO ADMIN ONLY.
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VacationResponseDto>> getVacationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<VacationResponseDto> vacations = vacationService.getVacationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(vacations);
    }

    /**
     * Updates an existing vacation entirely.
     * ALLOWED FOR: ADMIN, or MANAGER of the vacation's department.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageVacation(#id)")
    public ResponseEntity<VacationResponseDto> updateVacation(
            @PathVariable Long id,
            @Valid @RequestBody VacationCreateRequest request) {
        VacationResponseDto response = vacationService.updateVacation(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a vacation.
     * ALLOWED FOR: ADMIN, or MANAGER of the vacation's department.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageVacation(#id)")
    public ResponseEntity<Void> deleteVacation(@PathVariable Long id) {
        vacationService.deleteVacation(id);
        return ResponseEntity.noContent().build();
    }
}
