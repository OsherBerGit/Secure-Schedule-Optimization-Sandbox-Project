package com.example.mainbackend.algorithm.controller;

import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.algorithm.service.SchedulingConfigurationService;
import com.example.mainbackend.security.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduling-configs")
@RequiredArgsConstructor
public class SchedulingConfigurationController {

    private final SchedulingConfigurationService service;
    private final SecurityHelper securityHelper;

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulingConfigurationDto> getActiveConfig(Authentication authentication) {
        return ResponseEntity.ok(service.getActiveConfiguration(authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulingConfigurationDto> saveConfig(@Valid @RequestBody SchedulingConfigurationDto dto, Authentication authentication) {
        return ResponseEntity.ok(service.saveConfiguration(dto, authentication.getName(), securityHelper.isAdmin()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<SchedulingConfigurationDto>> getAllConfigs(Authentication authentication) {
        return ResponseEntity.ok(service.getAllConfigurations(authentication.getName(), securityHelper.isAdmin()));
    }
}
