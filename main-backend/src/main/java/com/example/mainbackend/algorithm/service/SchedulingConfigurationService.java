package com.example.mainbackend.algorithm.service;

import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.algorithm.entity.SchedulingConfiguration;
import com.example.mainbackend.algorithm.mapper.SchedulingConfigurationMapper;
import com.example.mainbackend.algorithm.repository.SchedulingConfigurationRepository;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages scheduling configurations.
 * Only one configuration may be active at a time.
 */
@Service
@AllArgsConstructor
public class SchedulingConfigurationService {

    private final SchedulingConfigurationRepository repository;
    private final SchedulingConfigurationMapper mapper;
    private final UserRepository userRepository;

    /**
     * Returns the currently active configuration.
     * Falls back to a safe in-memory default if the database is empty —
     * this ensures the scheduling engine always has valid weights to work with.
     */
    @Transactional(readOnly = true)
    public SchedulingConfigurationDto getActiveConfiguration(String nationalId) {
        return repository.findByIsActiveTrueAndCreatedBy_NationalId(nationalId)
                .map(mapper::mapToDto)
                .orElseGet(() -> repository.findByIsActiveTrue().stream()
                        .findFirst()
                        .map(mapper::mapToDto)
                        .orElseGet(this::getDefaultConfiguration));
    }

    /**
     * Returns a specific configuration by its ID.
     * Falls back to the active configuration if the ID is null.
     */
    @Transactional(readOnly = true)
    public SchedulingConfigurationDto getConfigurationById(Long id, String nationalId) {
        if (id == null) return getActiveConfiguration(nationalId);

        return repository.findById(id)
                .map(mapper::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
    }

    /**
     * Returns configurations based on role:
     * ADMIN sees all configurations.
     * MANAGER sees only their own configurations.
     */
    @Transactional(readOnly = true)
    public java.util.List<SchedulingConfigurationDto> getAllConfigurations(String nationalId, boolean isAdmin) {
        if (isAdmin)
            return repository.findAll().stream()
                    .map(mapper::mapToDto)
                    .toList();

        return repository.findByCreatedBy_NationalId(nationalId).stream()
                    .map(mapper::mapToDto)
                    .toList();
    }

    /**
     * Persists a new or updated configuration.
     *
     * Concurrency / transaction note:
     *   deactivateAll() runs an UPDATE inside the same transaction. The subsequent
     *   save() then writes the new record in the same flush, so there is never a
     *   window where zero configurations are active within a committed transaction.
     *   clearAutomatically = true on the @Modifying query clears the 1st-level cache
     *   so the entity loaded immediately after deactivateAll reflects the DB state.
     */
    @Transactional
    public SchedulingConfigurationDto saveConfiguration(SchedulingConfigurationDto dto, String nationalId, boolean isAdmin) {
        User creator = userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + nationalId));

        if (dto.isActive()) {
            if (isAdmin)
                repository.deactivateAll();
            else
                repository.deactivateAllByNationalId(nationalId);
        }

        SchedulingConfiguration entity = mapper.mapToEntity(dto);
        entity.setCreatedBy(creator);

        SchedulingConfiguration saved = repository.save(mapper.mapToEntity(dto));
        return mapper.mapToDto(saved);
    }

    /**
     * Safe in-memory fallback — returned when no configuration is seeded in the DB.
     * This is never persisted; it only keeps the algorithm from crashing on first boot.
     */
    private SchedulingConfigurationDto getDefaultConfiguration() {
        return new SchedulingConfigurationDto(
                null, "Default",
                0.4, 0.4, 0.2,
                true, 100, 500,
                0.1, 0.9, 0.2,
                "System",
                null
        );
    }
}
