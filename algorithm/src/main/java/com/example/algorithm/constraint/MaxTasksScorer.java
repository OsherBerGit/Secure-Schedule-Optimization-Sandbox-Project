package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoSchedulingConfiguration;

/**
 * Max Tasks Scorer — Capacity Penalty.
 *
 * <p>Rule: Penalizes assignments that cause a worker to exceed their preferred
 * maximum task limit for the current scheduling run.</p>
 *
 * <p>Logic:
 * <ol>
 * <li>If the worker has no {@code maxTasks} limit defined, the score is {@code 0} (perfect).</li>
 * <li>If the worker's currently assigned tasks (from {@code ctx.assignedCount()})
 * are equal to or greater than their limit, a base penalty is applied.</li>
 * <li>The base penalty is multiplied by {@code config.weightFairness} to allow
 * users to determine how strictly this preference should be enforced.</li>
 * </ol>
 * </p>
 *
 * <p>Zero-Trust: accesses only anonymous worker IDs and integer counts.
 * Prevents burnout mathematically without knowing the employee's identity.</p>
 */
public class MaxTasksScorer implements Scorer {

    private static final double BASE_PENALTY = -100.0;

    @Override
    public String getName() { return "MaxTasksScorer (Capacity Limit Penalty)"; }

    @Override
    public double score(ConstraintContext ctx, AlgoSchedulingConfiguration config) {
        Integer limit = ctx.candidate().getMaxTasks();

        // Rule 1: No limit defined
        if (limit == null)
            return 0.0;

        int currentLoad = ctx.assignedCount().getOrDefault(ctx.candidate().getId(), 0);

        // Rule 2 & 3: Exceeding limit results in a weighted penalty
        if (currentLoad >= limit)
            return BASE_PENALTY * config.getWeightFairness();

        return 0.0; // Under the limit, no penalty
    }
}