package com.example.algorithm.constraint;

// Strategy-pattern interface for modular constraint checking.
public interface ConstraintChecker {

    // Evaluates whether assigning the task to the candidate in the given context satisfies this constraint.
    ConstraintResult check(ConstraintContext context);
    String getName();
}

