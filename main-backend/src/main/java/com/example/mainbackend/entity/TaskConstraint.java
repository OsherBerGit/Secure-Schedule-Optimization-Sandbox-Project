package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a constraint/dependency between two tasks.
 * Used by the scheduling algorithm for topological sort and dependency resolution.
 *
 * Example: If Task B cannot start before Task A finishes, then:
 * - predecessorTask = Task A
 * - successorTask = Task B
 * - constraintType = FINISH_TO_START
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_constraint", indexes = {
    @Index(name = "idx_constraint_predecessor", columnList = "predecessor_task_id"),
    @Index(name = "idx_constraint_successor", columnList = "successor_task_id"),
    @Index(name = "idx_constraint_type", columnList = "constraint_type_id")
})
public class TaskConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * The task that must be completed/started first.
     * In a dependency graph, this is the "from" node.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_task_id", nullable = false)
    private Task predecessorTask;

    /**
     * The task that depends on the predecessor.
     * In a dependency graph, this is the "to" node.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_task_id", nullable = false)
    private Task successorTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constraint_type_id", nullable = false)
    private ConstraintType constraintType;

    /**
     * Optional lag time in minutes between tasks.
     * Positive value means delay (e.g., Task B starts 30 min after Task A ends).
     * Negative value means overlap is allowed.
     * Default is 0 (immediate).
     */
    @Column(name = "lag_minutes")
    @Builder.Default
    private Integer lagMinutes = 0;
}
