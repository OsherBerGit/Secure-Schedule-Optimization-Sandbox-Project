package com.example.sidebackend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

// Scheduling algorithm configuration: weights and GA parameters.
public record SchedulingConfigurationDto(

        @DecimalMin(value = "0.0", message = "weightPriority must be >= 0.0")
        @DecimalMax(value = "1.0", message = "weightPriority must be <= 1.0")
        double weightPriority,

        @DecimalMin(value = "0.0", message = "weightDeadline must be >= 0.0")
        @DecimalMax(value = "1.0", message = "weightDeadline must be <= 1.0")
        double weightDeadline,

        @DecimalMin(value = "0.0", message = "weightFairness must be >= 0.0")
        @DecimalMax(value = "1.0", message = "weightFairness must be <= 1.0")
        double weightFairness,

        @Min(value = 10, message = "populationSize must be at least 10")
        Integer populationSize,

        @Min(value = 1, message = "maxGenerations must be at least 1")
        Integer maxGenerations,

        @DecimalMin(value = "0.0", message = "mutationRate must be >= 0.0")
        @DecimalMax(value = "1.0", message = "mutationRate must be <= 1.0")
        double mutationRate,

        @DecimalMin(value = "0.0", message = "crossoverRate must be >= 0.0")
        @DecimalMax(value = "1.0", message = "crossoverRate must be <= 1.0")
        double crossoverRate,

        @DecimalMin(value = "0.0", message = "localSearchFrequency must be >= 0.0")
        @DecimalMax(value = "1.0", message = "localSearchFrequency must be <= 1.0")
        double localSearchFrequency

) {}
