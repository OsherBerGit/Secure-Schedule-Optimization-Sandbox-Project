package com.example.algorithm.engine;

import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.TaskAssignment;
import java.util.List;

/**
 * Strategy pattern interface for schedule optimization.
 * Two implementations:
 *   - GreedySchedulingStrategy     : highest priority first, best-fit employee
 *   - RoundRobinSchedulingStrategy : distributes tasks evenly across employees
 */
public interface SchedulingStrategy {
    List<TaskAssignment> schedule(ScheduleData data);
    String getName();
}
