package com.example.mainbackend.controller;

import com.example.mainbackend.dto.constrainttype.ConstraintTypeResponseDto;
import com.example.mainbackend.service.ConstraintTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/constraint-types")
@RequiredArgsConstructor
public class ConstraintTypeController {

    private final ConstraintTypeService constraintTypeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConstraintTypeResponseDto>> getAllConstraintTypes() {
        return ResponseEntity.ok(constraintTypeService.getAllConstraintTypes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConstraintTypeResponseDto> getConstraintTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(constraintTypeService.getConstraintTypeById(id));
    }
}

