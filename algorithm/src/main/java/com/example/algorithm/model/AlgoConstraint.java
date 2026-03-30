package com.example.algorithm.model;

public record AlgoConstraint(
        Long predecessorId,
        ConstraintType type
) { }
