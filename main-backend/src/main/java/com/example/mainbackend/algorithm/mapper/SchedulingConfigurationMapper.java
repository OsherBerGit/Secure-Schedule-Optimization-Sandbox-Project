package com.example.mainbackend.algorithm.mapper;

import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.algorithm.entity.SchedulingConfiguration;
import org.springframework.stereotype.Component;

@Component
public class SchedulingConfigurationMapper {

    public SchedulingConfiguration mapToEntity(SchedulingConfigurationDto dto) {
        return SchedulingConfiguration.builder()
                .id(dto.getId())                           // null for new records → AUTO generates id
                .configName(dto.getConfigName())
                .weightPriority(dto.getWeightPriority())
                .weightDeadline(dto.getWeightDeadline())
                .weightFairness(dto.getWeightFairness())
                .isActive(dto.isActive())
                .populationSize(dto.getPopulationSize())
                .maxGenerations(dto.getMaxGenerations())   // field was "generations" — now unified
                .build();
    }

    public SchedulingConfigurationDto mapToDto(SchedulingConfiguration entity) {
        return new SchedulingConfigurationDto(
                entity.getId(),
                entity.getConfigName(),
                entity.getWeightPriority(),
                entity.getWeightDeadline(),
                entity.getWeightFairness(),
                entity.isActive(),
                entity.getPopulationSize(),
                entity.getMaxGenerations()
        );
    }

}
