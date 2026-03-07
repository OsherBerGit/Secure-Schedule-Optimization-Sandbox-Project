package com.example.mainbackend.algorithm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating / updating a SchedulingConfiguration.
 * Weight fields must each be in [0.0, 1.0].
 * No Lombok — manual getters, setters, and constructors per project rules.
 */

// TODO: Replace the manual implementation with JPA annotations and Lombok once the entity design is finalized and stable.

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

    // ── Constructors ──────────────────────────────────────────────────────────

    public SchedulingConfigurationDto() {}

    public SchedulingConfigurationDto(
            Long id, String configName,
            double weightPriority, double weightDeadline, double weightFairness,
            boolean isActive, Integer populationSize, Integer maxGenerations) {
        this.id             = id;
        this.configName     = configName;
        this.weightPriority = weightPriority;
        this.weightDeadline = weightDeadline;
        this.weightFairness = weightFairness;
        this.isActive       = isActive;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }
    public String getConfigName()                    { return configName; }
    public void setConfigName(String configName)     { this.configName = configName; }
    public double getWeightPriority()                { return weightPriority; }
    public void setWeightPriority(double w)          { this.weightPriority = w; }
    public double getWeightDeadline()                { return weightDeadline; }
    public void setWeightDeadline(double w)          { this.weightDeadline = w; }
    public double getWeightFairness()                { return weightFairness; }
    public void setWeightFairness(double w)          { this.weightFairness = w; }
    public boolean isActive()                        { return isActive; }
    public void setActive(boolean isActive)          { this.isActive = isActive; }
    public Integer getPopulationSize()               { return populationSize; }
    public void setPopulationSize(Integer size)      { this.populationSize = size; }
    public Integer getMaxGenerations()               { return maxGenerations; }
    public void setMaxGenerations(Integer g)         { this.maxGenerations = g; }
}
