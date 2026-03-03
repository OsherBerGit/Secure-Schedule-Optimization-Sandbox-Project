package com.example.algorithm.engine;

import com.example.algorithm.db.ScheduleData;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Greedy Scheduling Strategy.
 *
 * Processes tasks in priority order (CRITICAL first, earliest deadline as tiebreak).
 * For each task, picks the least-loaded eligible employee who:
 *   - Has a required role for the task
 *   - Is not on vacation during the task window
 *   - Has not exceeded their maxTasks limit
 * Task dependency constraints are respected (predecessor must finish first).
 */
public class GreedySchedulingStrategy extends BaseSchedulingStrategy {

    @Override
    public String getName() {
        return "Greedy (Best-Fit by Priority)";
    }

    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<TaskAssignment> assignments = new ArrayList<>();
        Map<Long, Integer> assignedCount = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();

        List<AlgoTask> sortedTasks = getSortedUnassignedTasks(data.tasks());

        for (AlgoTask task : sortedTasks) {
            LocalDateTime start = calcStartTime(task, completionTimes);
            LocalDateTime end   = calcEndTime(start, task);

            List<AlgoUser> eligible = getEligibleEmployees(
                    task, data.users(), assignedCount, start, end);

            if (eligible.isEmpty()) {
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .scheduledStart(start)
                        .scheduledEnd(end)
                        .reason("No eligible employee found")
                        .build());
                completionTimes.put(task.getId(), end);
                continue;
            }

            // Pick the least-loaded eligible employee
            AlgoUser best = eligible.stream()
                    .min(Comparator.comparingInt(
                            u -> assignedCount.getOrDefault(u.getId(), 0)))
                    .orElseThrow();

            int prevCount = assignedCount.getOrDefault(best.getId(), 0);
            assignedCount.merge(best.getId(), 1, Integer::sum);
            completionTimes.put(task.getId(), end);

            assignments.add(TaskAssignment.builder()
                    .task(task)
                    .assignedEmployee(best)
                    .scheduledStart(start)
                    .scheduledEnd(end)
                    .reason(String.format(
                            "Greedy: least loaded (%d task(s) before this)", prevCount))
                    .build());
        }

        return assignments;
    }
}
