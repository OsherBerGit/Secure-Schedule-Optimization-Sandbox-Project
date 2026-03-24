package com.example.mainbackend.algorithm.controller;

import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.algorithm.service.SchedulingConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
public class SchedulingConfigurationController {

    private final SchedulingConfigurationService service;

    public SchedulingConfigurationController(SchedulingConfigurationService service) {
        this.service = service;
    }

    /**
     * Returns the currently active configuration.
     * Accessible by any authenticated user (workers need to know the active strategy).
     * Authentication is enforced by the global security filter — no anonymous access.
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SchedulingConfigurationDto> getActiveConfig() {
        return ResponseEntity.ok(service.getActiveConfiguration());
    }

    /**
     * Creates or replaces the active scheduling configuration.
     * Restricted to ADMIN only.
     *
     * @param dto validated configuration payload
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchedulingConfigurationDto> saveConfig(
            @Valid @RequestBody SchedulingConfigurationDto dto) {
        return ResponseEntity.ok(service.saveConfiguration(dto));
    }

    /**
     * Returns all available configurations.
     * Accessible by ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<SchedulingConfigurationDto>> getAllConfigs() {
        return ResponseEntity.ok(service.getAllConfigurations());
    }
}