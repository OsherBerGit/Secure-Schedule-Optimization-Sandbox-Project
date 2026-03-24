package com.example.mainbackend.algorithm.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores a named set of weights and algorithm parameters used by the scheduling engine.
 * Only one configuration may be active at a time (enforced by deactivateAll before save).
 */
@Getter
@Setter
@Builder
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

    /** Probability of mutation per gene (0.0 – 1.0). */
    private double mutationRate;

    /** Probability of crossover per parent pair (0.0 – 1.0). */
    private double crossoverRate;

    /** Frequency of local search application (0.0 – 1.0). */
    private double localSearchFrequency;
}
