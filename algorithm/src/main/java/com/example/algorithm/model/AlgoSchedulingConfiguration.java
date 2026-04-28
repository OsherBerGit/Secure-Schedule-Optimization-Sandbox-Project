package com.example.algorithm.model;

// Scheduling algorithm configuration — weights and GA parameters.
public final class AlgoSchedulingConfiguration {

    private final double weightPriority;
    private final double weightDeadline;
    private final double weightFairness;
    private final double mutationRate;
    private final double crossoverRate;
    private final double localSearchFrequency;
    private final Integer populationSize;
    private final Integer maxGenerations;

    public AlgoSchedulingConfiguration(double weightPriority, double weightDeadline, double weightFairness, Integer populationSize,
                                        Integer maxGenerations, double mutationRate, double crossoverRate, double localSearchFrequency) {
        this.weightPriority = weightPriority;
        this.weightDeadline = weightDeadline;
        this.weightFairness = weightFairness;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.localSearchFrequency = localSearchFrequency;
    }

    public double getWeightPriority() { return weightPriority; }
    public double getWeightDeadline() { return weightDeadline; }
    public double getWeightFairness() { return weightFairness; }
    public double getMutationRate() { return mutationRate; }
    public double getCrossoverRate() { return crossoverRate; }
    public double getLocalSearchFrequency() { return localSearchFrequency; }
    public Integer getPopulationSize() { return populationSize; }
    public Integer getMaxGenerations() { return maxGenerations; }

    @Override
    public String toString() {
        return "AlgoSchedulingConfiguration{weightPriority=" + weightPriority
                + ", weightDeadline=" + weightDeadline
                + ", weightFairness=" + weightFairness
                + ", mutationRate=" + mutationRate
                + ", crossoverRate=" + crossoverRate
                + ", localSearchFrequency=" + localSearchFrequency
                + ", populationSize=" + populationSize
                + ", maxGenerations=" + maxGenerations + '}';
    }
}
