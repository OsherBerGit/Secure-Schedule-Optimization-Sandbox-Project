package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TaskConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_task_id", nullable = false)
    private Task firstTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "second_task_id", nullable = false)
    private Task secondTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constraint_type_id", nullable = false)
    private ConstraintType constraintType;
}
