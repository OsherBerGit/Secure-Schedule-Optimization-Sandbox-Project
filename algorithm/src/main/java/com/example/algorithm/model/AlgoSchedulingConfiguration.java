package com.example.algorithm.model;

/**
 * Scheduling algorithm configuration — weights and GA parameters.
 *
 * <p>Mirrors {@code SchedulingConfigurationDto} exactly so that the mapper
 * performs a lossless, field-by-field copy with no transformation required.</p>
 *
 * <p>Field alignment with {@code SchedulingConfigurationDto}:</p>
 * <ul>
 *   <li>{@code weightPriority}  — weight for task priority in the scoring function [0.0–1.0]</li>
 *   <li>{@code weightDeadline}  — weight for deadline urgency in the scoring function [0.0–1.0]</li>
 *   <li>{@code weightFairness}  — weight for workload fairness across workers [0.0–1.0]</li>
 *   <li>{@code populationSize}  — genetic-algorithm population size (minimum 10)</li>
 *   <li>{@code maxGenerations}  — genetic-algorithm generation limit (minimum 1)</li>
 * </ul>
 *
 * <p>Pure Java: no Spring, Jackson, or Lombok annotations.
 * Immutable: all fields are final.</p>
 */
public final class AlgoSchedulingConfiguration {

    private final double  weightPriority;
    private final double  weightDeadline;
    private final double  weightFairness;
    private final Integer populationSize;
    private final Integer maxGenerations;

    public AlgoSchedulingConfiguration(double weightPriority,
                                        double weightDeadline,
                                        double weightFairness,
                                        Integer populationSize,
                                        Integer maxGenerations) {
        this.weightPriority = weightPriority;
        this.weightDeadline = weightDeadline;
        this.weightFairness = weightFairness;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
    }

    public double  getWeightPriority()  { return weightPriority; }
    public double  getWeightDeadline()  { return weightDeadline; }
    public double  getWeightFairness()  { return weightFairness; }
    public Integer getPopulationSize()  { return populationSize; }
    public Integer getMaxGenerations()  { return maxGenerations; }

    @Override
    public String toString() {
        return "AlgoSchedulingConfiguration{weightPriority=" + weightPriority
                + ", weightDeadline=" + weightDeadline
                + ", weightFairness=" + weightFairness
                + ", populationSize=" + populationSize
                + ", maxGenerations=" + maxGenerations + '}';
    }
}

