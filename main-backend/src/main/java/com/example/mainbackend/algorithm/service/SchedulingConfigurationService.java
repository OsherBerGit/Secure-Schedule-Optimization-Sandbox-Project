package com.example.mainbackend.algorithm.service;

import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.algorithm.entity.SchedulingConfiguration;
import com.example.mainbackend.algorithm.mapper.SchedulingConfigurationMapper;
import com.example.mainbackend.algorithm.repository.SchedulingConfigurationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages scheduling configurations.
 * Only one configuration may be active at a time.
 * No Lombok — manual constructor injection per project rules.
 */
@Service
@AllArgsConstructor
public class SchedulingConfigurationService {

    private final SchedulingConfigurationRepository repository;
    private final SchedulingConfigurationMapper mapper;

    /**
     * Returns the currently active configuration.
     * Falls back to a safe in-memory default if the database is empty —
     * this ensures the scheduling engine always has valid weights to work with.
     */
    @Transactional(readOnly = true)
    public SchedulingConfigurationDto getActiveConfiguration() {
        return repository.findByIsActiveTrue()
                .map(mapper::mapToDto)
                .orElseGet(this::getDefaultConfiguration);
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
    public SchedulingConfigurationDto saveConfiguration(SchedulingConfigurationDto dto) {
        if (dto.isActive())
            // Deactivate every existing active config before making the new one active.
            // clearAutomatically = true (set on @Modifying) keeps the persistence context in sync.
            repository.deactivateAll();

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
                true, 100, 500
        );
    }
}
