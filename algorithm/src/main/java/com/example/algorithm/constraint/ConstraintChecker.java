package com.example.algorithm.constraint;

/**
 * Strategy-pattern interface for modular constraint checking.
 *
 * Each implementation encapsulates exactly ONE scheduling rule.
 * Strategies iterate through all registered checkers before committing
 * any assignment — if any checker returns a failing {@link ConstraintResult},
 * the candidate is rejected.
 *
 * Zero-Trust: implementations may ONLY reference classes in
 * {@code com.example.algorithm.model} and {@code com.example.algorithm.constraint}.
 * No database access, no HTTP calls, no real identity data.
 */
public interface ConstraintChecker {

    /**
     * Evaluates whether assigning the task to the candidate in the given context
     * satisfies this constraint.
     *
     * @param context all data required to evaluate the constraint
     * @return {@link ConstraintResult#pass()} if the constraint is satisfied,
     *         {@link ConstraintResult#fail(String)} with a reason if not
     */
    ConstraintResult check(ConstraintContext context);

    /**
     * Short human-readable name for this constraint (used in logs and reasons).
     * Example: "PrecedenceConstraint", "AvailabilityConstraint"
     */
    String getName();
}

