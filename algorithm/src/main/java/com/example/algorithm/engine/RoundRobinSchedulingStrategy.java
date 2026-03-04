package com.example.algorithm.engine;

import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.db.ScheduleData;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Round-Robin Scheduling Strategy.
 *
 * Distributes tasks evenly across eligible employees by cycling through them in order.
 * Each task still respects the full constraint pipeline:
 *   - Required roles, vacation availability, max task limits (eligibility pre-filter)
 *   - PrecedenceConstraint  — predecessor tasks must finish first
 *   - AvailabilityConstraint — vacation overlap and maxTasks double-check
 *
 * If the round-robin pick fails the constraint pipeline, the next eligible
 * candidate (in round-robin order) is tried until one passes or all are exhausted.
 */
public class RoundRobinSchedulingStrategy extends BaseSchedulingStrategy {

    @Override
    public String getName() {
        return "Round-Robin (Fair Distribution)";
    }

    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<TaskAssignment> assignments = new ArrayList<>();
        Map<Long, Integer>       assignedCount   = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();

        List<AlgoUser> allUsers = new ArrayList<>(data.users());
        int roundRobinIndex = 0;

        List<AlgoTask> sortedTasks = getSortedUnassignedTasks(data.tasks());

        for (AlgoTask task : sortedTasks) {
            LocalDateTime start = calcStartTime(task, completionTimes);
            LocalDateTime end   = calcEndTime(start, task);

            // Pre-filter: role, vacation overlap, maxTasks limit
            List<AlgoUser> eligible = getEligibleEmployees(
                    task, allUsers, assignedCount, start, end);

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

            // Constraint pipeline: iterate from the current round-robin position,
            // pick the first candidate in that order who passes all constraints
            AlgoUser picked           = null;
            ConstraintResult lastFail = null;
            int size = allUsers.size();

            for (int i = 0; i < size; i++) {
                AlgoUser candidate = allUsers.get((roundRobinIndex + i) % size);
                if (!eligible.contains(candidate)) continue;

                ConstraintResult result = runConstraints(
                        task, candidate, start, end, completionTimes, assignedCount);
                if (result.isValid()) {
                    picked = candidate;
                    // Advance round-robin index past this pick
                    roundRobinIndex = (allUsers.indexOf(picked) + 1) % size;
                    break;
                }
                lastFail = result;
            }

            if (picked == null) {
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

            assignedCount.merge(picked.getId(), 1, Integer::sum);
            completionTimes.put(task.getId(), end);

            assignments.add(TaskAssignment.builder()
                    .task(task)
                    .assignedEmployee(picked)
                    .scheduledStart(start)
                    .scheduledEnd(end)
                    .reason(String.format(
                            "Round-Robin: turn-based pick (now has %d task(s))",
                            assignedCount.get(picked.getId())))
                    .build());
        }

        return assignments;
    }
}
