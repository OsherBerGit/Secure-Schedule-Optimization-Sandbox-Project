package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoSchedulingConfiguration;

// Strategy-pattern interface for modular Soft Constraint scoring.
public interface Scorer {
    //Evaluates the assignment candidate and returns a penalty score.
    double score(ConstraintContext ctx, AlgoSchedulingConfiguration config);

    String getName();
}
