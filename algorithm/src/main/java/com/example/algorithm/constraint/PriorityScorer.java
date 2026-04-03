package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoSchedulingConfiguration;

/**
 * Priority Scorer — High-Value Task Prioritization.
 *
 * <p>Rule: Rewards the system for successfully assigning high-priority tasks.
 * The higher the task priority, the more weight this assignment carries in the
 * overall fitness calculation.</p>
 *
 * <p>Logic:
 * The score is calculated as: {@code task.priority * config.weightPriority}.
 * This ensures that even if a schedule has minor flaws (like a slight fairness
 * imbalance), it will still be preferred if it covers all "Critical" tasks.</p>
 *
 * <p>Zero-Trust: Only uses the numeric priority value and the task ID.</p>
 */
public class PriorityScorer implements Scorer {

    @Override
    public String getName() { return "PriorityScorer (Task Importance)"; }

    @Override
    public double score(ConstraintContext ctx, AlgoSchedulingConfiguration config) {
        // Higher priority (e.g., 5) results in a better (higher) score.
        // We multiply by the weight set in the configuration by the user.
        Integer priorityObj = ctx.task().getPriorityLevel();
        double taskPriority = (priorityObj != null) ? priorityObj : 0.0;

        return taskPriority * config.getWeightPriority();
    }
}