package com.example.mainbackend.entity;

import jakarta.persistence.*;

@Entity
public class TaskConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "first_task_id", nullable = false)
    private Task firstTask;

    @ManyToOne
    @JoinColumn(name = "second_task_id", nullable = false)
    private Task secondTask;

    // add as a table
    private String constraintType;
}
