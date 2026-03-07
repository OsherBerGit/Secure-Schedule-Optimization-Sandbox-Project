package com.example.mainbackend.algorithm.entity;

import jakarta.persistence.*;

/**
 * Stores a named set of weights and algorithm parameters used by the scheduling engine.
 * Only one configuration may be active at a time (enforced by deactivateAll before save).
 */
@Entity
@Table(name = "scheduling_configuration")
public class SchedulingConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** Human-readable label, e.g. "Default", "High Pressure", "Fairness Focus". */
    @Column(nullable = false)
    private String configName;

    /** Weight for task priority score (0.0 – 1.0). */
    @Column(nullable = false)
    private double weightPriority;

    /** Weight for deadline urgency score (0.0 – 1.0). */
    @Column(nullable = false)
    private double weightDeadline;

    /** Weight for workload-fairness score (0.0 – 1.0). */
    @Column(nullable = false)
    private double weightFairness;

    /** True if this is the currently active configuration. */
    @Column(nullable = false)
    private boolean isActive;

    /** Population size for genetic / evolutionary algorithm variants. */
    private Integer populationSize;

    /** Number of generations / iterations for evolutionary algorithm variants. */
    private Integer maxGenerations;   // NOTE: was "generations" — unified to maxGenerations across entity + DTO

    // ── Constructors ──────────────────────────────────────────────────────────

    public SchedulingConfiguration() {}

    private SchedulingConfiguration(Builder b) {
        this.id             = b.id;
        this.configName     = b.configName;
        this.weightPriority = b.weightPriority;
        this.weightDeadline = b.weightDeadline;
        this.weightFairness = b.weightFairness;
        this.isActive       = b.isActive;
        this.populationSize = b.populationSize;
        this.maxGenerations = b.maxGenerations;
    }

    // ── Manual Builder ────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String configName;
        private double weightPriority;
        private double weightDeadline;
        private double weightFairness;
        private boolean isActive;
        private Integer populationSize;
        private Integer maxGenerations;

        private Builder() {}

        public Builder id(Long id)                             { this.id = id; return this; }
        public Builder configName(String configName)           { this.configName = configName; return this; }
        public Builder weightPriority(double w)                { this.weightPriority = w; return this; }
        public Builder weightDeadline(double w)                { this.weightDeadline = w; return this; }
        public Builder weightFairness(double w)                { this.weightFairness = w; return this; }
        public Builder isActive(boolean isActive)              { this.isActive = isActive; return this; }
        public Builder populationSize(Integer size)            { this.populationSize = size; return this; }
        public Builder maxGenerations(Integer maxGenerations)  { this.maxGenerations = maxGenerations; return this; }

        public SchedulingConfiguration build()                 { return new SchedulingConfiguration(this); }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public String getConfigName()                { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public double getWeightPriority()            { return weightPriority; }
    public void setWeightPriority(double w)      { this.weightPriority = w; }
    public double getWeightDeadline()            { return weightDeadline; }
    public void setWeightDeadline(double w)      { this.weightDeadline = w; }
    public double getWeightFairness()            { return weightFairness; }
    public void setWeightFairness(double w)      { this.weightFairness = w; }
    public boolean isActive()                    { return isActive; }
    public void setActive(boolean isActive)      { this.isActive = isActive; }
    public Integer getPopulationSize()           { return populationSize; }
    public void setPopulationSize(Integer s)     { this.populationSize = s; }
    public Integer getMaxGenerations()           { return maxGenerations; }
    public void setMaxGenerations(Integer g)     { this.maxGenerations = g; }
}
