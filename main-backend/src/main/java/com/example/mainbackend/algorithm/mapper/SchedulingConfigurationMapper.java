package com.example.mainbackend.algorithm.mapper;

import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.algorithm.entity.SchedulingConfiguration;
import org.springframework.stereotype.Component;

@Component
public class SchedulingConfigurationMapper {
    public SchedulingConfiguration mapToEntity(SchedulingConfigurationDto dto) {
        if (dto == null) return null;

        return SchedulingConfiguration.builder()
                .id(dto.getId())
                .configName(dto.getConfigName())
                .weightPriority(dto.getWeightPriority())
                .weightDeadline(dto.getWeightDeadline())
                .weightFairness(dto.getWeightFairness())
                .isActive(dto.isActive())
                .populationSize(dto.getPopulationSize())
                .maxGenerations(dto.getMaxGenerations())
                .mutationRate(dto.getMutationRate())
                .crossoverRate(dto.getCrossoverRate())
                .localSearchFrequency(dto.getLocalSearchFrequency())
                .build();
    }

    public SchedulingConfigurationDto mapToDto(SchedulingConfiguration entity) {
        if (entity == null) return null;

        Long creatorId = null;
        String creatorName = "System";

        if (entity.getCreatedBy() != null) {
            creatorId = entity.getCreatedBy().getId();
            creatorName = entity.getCreatedBy().getFirstName() + " " + entity.getCreatedBy().getLastName();
        }

        return new SchedulingConfigurationDto(
                entity.getId(),
                entity.getConfigName(),
                entity.getWeightPriority(),
                entity.getWeightDeadline(),
                entity.getWeightFairness(),
                entity.isActive(),
                entity.getPopulationSize(),
                entity.getMaxGenerations(),
                entity.getMutationRate(),
                entity.getCrossoverRate(),
                entity.getLocalSearchFrequency(),
                creatorName,
                creatorId
        );
    }
}
