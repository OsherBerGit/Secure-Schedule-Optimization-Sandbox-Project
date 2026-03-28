package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoSchedulingConfiguration;

/**
 * Strategy-pattern interface for modular Soft Constraint scoring.
 *
 * <p>Unlike {@link ConstraintChecker} which enforces strict pass/fail rules (Hard Constraints),
 * a {@code Scorer} evaluates preferences and organizational goals. It returns a numerical
 * penalty indicating how "bad" an assignment is regarding a specific goal.</p>
 *
 * <p>Usage:
 * Primarily utilized by advanced engines (like Memetic Algorithms) to calculate the fitness
 * score of a complete schedule. Greedy algorithms typically ignore scorers.</p>
 *
 * <p>Zero-Trust: implementations may ONLY reference anonymous models and configuration data.</p>
 */
public interface Scorer {
    /**
     * Evaluates the assignment candidate and returns a penalty score.
     *
     * @param ctx    the data context of the proposed assignment
     * @param config the scheduling configuration containing weights
     * @return {@code 0.0} if the preference is perfectly met, or a negative value
     * (e.g., {@code -100.0}) representing the penalty for violating the preference.
     */
    double score(ConstraintContext ctx, AlgoSchedulingConfiguration config);

    /**
     * Short human-readable name for this scorer (used in logs and analytics).
     * Example: "MaxTasksScorer", "FairnessScorer"
     */
    String getName();
}
