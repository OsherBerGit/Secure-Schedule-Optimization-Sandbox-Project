package com.example.sidebackend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/**
 * Scheduling algorithm configuration: weights and GA parameters.
 *
 * <p>All weight fields must be in the range [0.0, 1.0].
 * Received from the main-backend along with every scheduling request.</p>
 *
 * @param weightPriority   Weight assigned to task priority in the scoring function
 * @param weightDeadline   Weight assigned to deadline urgency in the scoring function
 * @param weightFairness   Weight assigned to workload fairness across workers
 * @param populationSize   Genetic-algorithm population size (minimum 10)
 * @param maxGenerations   Genetic-algorithm generation limit (minimum 1)
 */
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
