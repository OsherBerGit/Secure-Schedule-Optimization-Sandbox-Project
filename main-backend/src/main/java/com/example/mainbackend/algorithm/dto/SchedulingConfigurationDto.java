package com.example.mainbackend.algorithm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating / updating a SchedulingConfiguration.
 * Weight fields must each be in [0.0, 1.0].
 * No Lombok — manual getters, setters, and constructors per project rules.
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchedulingConfigurationDto {

    private Long id;

    @NotBlank(message = "Configuration name is required")
    private String configName;

    @DecimalMin(value = "0.0", message = "weightPriority must be >= 0.0")
    @DecimalMax(value = "1.0", message = "weightPriority must be <= 1.0")
    private double weightPriority;

    @DecimalMin(value = "0.0", message = "weightDeadline must be >= 0.0")
    @DecimalMax(value = "1.0", message = "weightDeadline must be <= 1.0")
    private double weightDeadline;

    @DecimalMin(value = "0.0", message = "weightFairness must be >= 0.0")
    @DecimalMax(value = "1.0", message = "weightFairness must be <= 1.0")
    private double weightFairness;

    private boolean isActive;

    @Min(value = 10, message = "populationSize must be at least 10")
    private Integer populationSize;

    @Min(value = 1, message = "maxGenerations must be at least 1")
    private Integer maxGenerations;

    @DecimalMin(value = "0.0", message = "mutationRate must be >= 0.0")
    @DecimalMax(value = "1.0", message = "mutationRate must be <= 1.0")
    private double mutationRate;

    @DecimalMin(value = "0.0", message = "crossoverRate must be >= 0.0")
    @DecimalMax(value = "1.0", message = "crossoverRate must be <= 1.0")
    private double crossoverRate;

    @DecimalMin(value = "0.0", message = "localSearchFrequency must be >= 0.0")
    @DecimalMax(value = "1.0", message = "localSearchFrequency must be <= 1.0")
    private double localSearchFrequency;
}
