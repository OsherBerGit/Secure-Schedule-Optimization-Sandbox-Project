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

@Service
@AllArgsConstructor
public class SchedulingConfigurationService {

    private final SchedulingConfigurationRepository repository;
    private final SchedulingConfigurationMapper mapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SchedulingConfigurationDto getActiveConfiguration(String nationalId) {
        return repository.findByIsActiveTrueAndCreatedBy_NationalId(nationalId)
                .map(mapper::mapToDto)
                .orElseGet(() -> repository.findByIsActiveTrue().stream()
                        .findFirst()
                        .map(mapper::mapToDto)
                        .orElseGet(this::getDefaultConfiguration));
    }

    @Transactional(readOnly = true)
    public SchedulingConfigurationDto getConfigurationById(Long id, String nationalId) {
        if (id == null) return getActiveConfiguration(nationalId);

        return repository.findById(id)
                .map(mapper::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
    }

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

        SchedulingConfiguration saved = repository.save(entity);
        return mapper.mapToDto(saved);
    }

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
