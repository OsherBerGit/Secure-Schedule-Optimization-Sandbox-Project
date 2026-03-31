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

/**
 * REST API for managing scheduling configurations.
 *
 * GET  /active  — requires authentication (any role); returns the active config.
 * POST /        — requires ROLE_ADMIN; creates or updates a configuration.
 *
 * Note: authentication is enforced globally via Spring Security filter chain.
 * The explicit @PreAuthorize on the write endpoint provides a defence-in-depth
 * role check on top of that filter.
 */
@RestController
@RequestMapping("/api/scheduling-configs")
@RequiredArgsConstructor
public class SchedulingConfigurationController {

    private final SchedulingConfigurationService service;
    private final SecurityHelper securityHelper;

    /**
     * Returns the currently active configuration.
     * Accessible by any authenticated user (workers need to know the active strategy).
     * Authentication is enforced by the global security filter — no anonymous access.
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulingConfigurationDto> getActiveConfig(Authentication authentication) {
        return ResponseEntity.ok(service.getActiveConfiguration(authentication.getName()));
    }

    /**
     * Creates or replaces the active scheduling configuration.
     * Restricted to ADMIN only.
     *
     * @param dto validated configuration payload
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SchedulingConfigurationDto> saveConfig(@Valid @RequestBody SchedulingConfigurationDto dto, Authentication authentication) {
        return ResponseEntity.ok(service.saveConfiguration(dto, authentication.getName(), securityHelper.isAdmin()));
    }

    /**
     * Returns all available configurations.
     * Accessible by ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<SchedulingConfigurationDto>> getAllConfigs(Authentication authentication) {
        return ResponseEntity.ok(service.getAllConfigurations(authentication.getName(), securityHelper.isAdmin()));
    }
}