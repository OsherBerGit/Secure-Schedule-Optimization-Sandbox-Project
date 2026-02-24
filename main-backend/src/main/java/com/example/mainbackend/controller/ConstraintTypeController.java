package com.example.mainbackend.controller;

import com.example.mainbackend.dto.constrainttype.ConstraintTypeRequest;
import com.example.mainbackend.dto.constrainttype.ConstraintTypeResponseDto;
import com.example.mainbackend.service.ConstraintTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing task constraint types.
 * Provides endpoints for CRUD operations on constraint type lookup table.
 */
@RestController
@RequestMapping("/api/constraint-types")
@RequiredArgsConstructor
public class ConstraintTypeController {

    private final ConstraintTypeService constraintTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConstraintTypeResponseDto> createConstraintType(@Valid @RequestBody ConstraintTypeRequest request) {
        ConstraintTypeResponseDto response = constraintTypeService.createConstraintType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<ConstraintTypeResponseDto> getConstraintTypeById(@PathVariable Long id) {
        ConstraintTypeResponseDto response = constraintTypeService.getConstraintTypeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<ConstraintTypeResponseDto>> getAllConstraintTypes() {
        List<ConstraintTypeResponseDto> constraintTypes = constraintTypeService.getAllConstraintTypes();
        return ResponseEntity.ok(constraintTypes);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConstraintTypeResponseDto> updateConstraintType(
            @PathVariable Long id,
            @Valid @RequestBody ConstraintTypeRequest request) {
        ConstraintTypeResponseDto response = constraintTypeService.updateConstraintType(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConstraintType(@PathVariable Long id) {
        constraintTypeService.deleteConstraintType(id);
        return ResponseEntity.noContent().build();
    }
}

