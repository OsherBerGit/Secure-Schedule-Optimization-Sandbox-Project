package com.example.mainbackend.algorithm.entity;

import com.example.mainbackend.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a named set of weights and algorithm parameters used by the scheduling engine.
 * Only one configuration may be active at a time (enforced by deactivateAll before save).
 */
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
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

    /**
     * References the `User` entity responsible for creating this scheduling configuration.
     * Represents a required relationship to ensure traceability of the configuration's origin.
     *
     * Maps to the `created_by_user_id` column in the database.
     * This column is non-nullable, meaning all scheduling configurations must have an associated creator.
     */
    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;
}
