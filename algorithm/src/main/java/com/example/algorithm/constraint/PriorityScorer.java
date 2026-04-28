package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoSchedulingConfiguration;

// Priority Scorer — High-Value Task Prioritization.
// Rewards the system for successfully assigning high-priority tasks.
// The higher the task priority, the more weight this assignment carries in the overall fitness calculation.
public class PriorityScorer implements Scorer {

    @Override
    public String getName() { return "PriorityScorer (Task Importance)"; }

    @Override
    public double score(ConstraintContext ctx, AlgoSchedulingConfiguration config) {
        // Higher priority (e.g., 5) results in a higher score.
        Integer priorityObj = ctx.task().getPriorityLevel();
        double taskPriority = (priorityObj != null) ? priorityObj : 0.0;

        return taskPriority * config.getWeightPriority();
    }
}