package com.example.algorithm.engine;

import com.example.algorithm.constraint.ConstraintResult;
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
 * For each task, picks the least-loaded eligible employee who passes ALL constraint checks:
 *   - Has a required role for the task          (eligibility pre-filter)
 *   - Is not on vacation during the task window (eligibility pre-filter + AvailabilityConstraint)
 *   - Has not exceeded their maxTasks limit     (eligibility pre-filter + AvailabilityConstraint)
 *   - All predecessor tasks have finished       (PrecedenceConstraint)
 *
 * If the best candidate fails the constraint pipeline, the next least-loaded
 * candidate is tried. If no candidate passes, the task is recorded as unassigned
 * with the constraint violation reason.
 */
public class GreedySchedulingStrategy extends BaseSchedulingStrategy {

    @Override
    public String getName() {
        return "Greedy (Best-Fit by Priority)";
    }

    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<TaskAssignment> assignments = new ArrayList<>();
        Map<Long, Integer>       assignedCount   = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();

        List<AlgoTask> sortedTasks = getSortedUnassignedTasks(data.tasks());

        for (AlgoTask task : sortedTasks) {
            LocalDateTime start = calcStartTime(task, completionTimes);
            LocalDateTime end   = calcEndTime(start, task);

            // Pre-filter: role, vacation overlap, maxTasks limit
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

            // Sort eligible candidates by current load (least-loaded first)
            eligible.sort(Comparator.comparingInt(
                    u -> assignedCount.getOrDefault(u.getId(), 0)));

            // Constraint pipeline: pick first candidate that passes all constraints
            AlgoUser best             = null;
            ConstraintResult lastFail = null;

            for (AlgoUser candidate : eligible) {
                ConstraintResult result = runConstraints(
                        task, candidate, start, end, completionTimes, assignedCount);
                if (result.isValid()) {
                    best = candidate;
                    break;
                }
                lastFail = result; // keep last failure reason for reporting
            }

            if (best == null) {
                // All candidates failed the constraint pipeline
                String reason = lastFail != null
                        ? "Constraint violation: " + lastFail.getReason()
                        : "All candidates failed constraint checks";
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .scheduledStart(start)
                        .scheduledEnd(end)
                        .reason(reason)
                        .build());
                completionTimes.put(task.getId(), end);
                continue;
            }

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
