package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_task_id", nullable = false)
    private Task predecessorTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_task_id", nullable = false)
    private Task successorTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constraint_type_id", nullable = false)
    private ConstraintType constraintType;

    @Column(name = "lag_minutes")
    @Builder.Default
    private Integer lagMinutes = 0;
}
