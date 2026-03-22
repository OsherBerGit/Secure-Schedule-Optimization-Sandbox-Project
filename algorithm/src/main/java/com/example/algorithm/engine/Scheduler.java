package com.example.algorithm.engine;

import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.TaskAssignment;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Context class for the Strategy pattern.
 *
 * Holds a SchedulingStrategy and delegates execution to it.
 * Switch strategies at runtime via setStrategy().
 *
 * Usage:
 *   Scheduler scheduler = new Scheduler(new GreedySchedulingStrategy());
 *   List<TaskAssignment> result = scheduler.run(data);
 *
 *   scheduler.setStrategy(new RoundRobinSchedulingStrategy());
 *   List<TaskAssignment> result2 = scheduler.run(data);
 */
public class Scheduler {

    private SchedulingStrategy strategy;

    public Scheduler(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    public SchedulingStrategy getStrategy() {
        return strategy;
    }

    public List<TaskAssignment> run(ScheduleData data) {
        System.out.println("Running strategy: " + strategy.getName());
        try {
            return strategy.schedule(data);
        } catch (Throwable e) {
            System.err.println("CRITICAL ERROR in Scheduler [" + strategy.getName() + "]: " + e.getMessage());
            e.printStackTrace();
            
            // Global Fail-Safe: If any algorithm crashes, return tasks as "Unscheduled"
            if (data.tasks() == null) return Collections.emptyList();
            return data.tasks().stream()
                    .map(t -> TaskAssignment.builder()
                            .task(t)
                            .reason("Algorithm Failed (" + strategy.getName() + "): " + e.getMessage())
                            .build())
                    .collect(Collectors.toList());
        }
    }
}
