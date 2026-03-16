package com.example.algorithm.engine;

import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.db.ScheduleData;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Simple "First-Fit" Greedy scheduling strategy.
 *
 * <p>Processes tasks in priority order (highest priority first, earliest deadline as tiebreak).
 * For each task, picks the least-loaded eligible employee who passes ALL constraint checks:</p>
 * <ul>
 *   <li>Has a required role for the task          (eligibility pre-filter)</li>
 *   <li>Is not on vacation during the task window (eligibility pre-filter + AvailabilityConstraint)</li>
 *   <li>Has not exceeded their maxTasks limit     (eligibility pre-filter + AvailabilityConstraint)</li>
 *   <li>All predecessor tasks have finished       (PrecedenceConstraint)</li>
 * </ul>
 *
 * <p>If the best candidate fails the constraint pipeline, the next least-loaded
 * candidate is tried. If no candidate passes, the task is recorded as unassigned
 * with the constraint violation reason.</p>
 *
 * <h3>Complexity Analysis</h3>
 * <ul>
 *   <li><b>Time Complexity:</b> O(T log T + T · W)</li>
 *   <li><b>Variables:</b>
 *     <ul>
 *       <li>T = Number of Tasks</li>
 *       <li>W = Number of Workers</li>
 *     </ul>
 *   </li>
 *   <li><b>Explanation:</b>
 *     <ul>
 *       <li>Sorting tasks by priority/deadline takes O(T log T).</li>
 *       <li>Iterating through sorted tasks takes O(T).</li>
 *       <li>For each task, we iterate through all workers (W) to find the best fit, performing constant-time checks (availability, roles) for each.</li>
 *       <li>Thus, the scheduling loop is O(T · W).</li>
 *       <li>Overall growth is linear with respect to workers and quasi-linear with respect to tasks.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class GreedySchedulingStrategy extends BaseSchedulingStrategy {

    @Override
    public String getName() {
        return "GREEDY";
    }

    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<TaskAssignment>     assignments   = new ArrayList<>();
        Map<Long, Integer>       assignedCount = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();

        List<AlgoTask> sortedTasks = getSortedUnassignedTasks(data.tasks());

        for (AlgoTask task : sortedTasks) {
            LocalDateTime start = calcStartTime(task, completionTimes);
            LocalDateTime end   = calcEndTime(start, task);

            // Pre-filter: role, vacation overlap, maxTasks limit
            List<AlgoUser> eligible = getEligibleEmployees(
                    task, data.users(), assignedCount, start, end);

            // Sort eligible workers by current workload (least-loaded first)
            eligible.sort(Comparator.comparingInt(u -> assignedCount.getOrDefault(u.getId(), 0)));

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

            // Constraint pipeline: iterate least-loaded first, pick the first that passes
            AlgoUser picked   = null;
            ConstraintResult lastFail = null;

            for (AlgoUser candidate : eligible) {
                ConstraintResult result = runConstraints(
                        task, candidate, start, end, completionTimes, assignedCount);
                if (result.isValid()) {
                    picked = candidate;
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
            completionTimes.put(task.getId(), end); // ← required: lets dependent tasks resolve this predecessor

            assignments.add(TaskAssignment.builder()
                    .task(task)
                    .assignedEmployee(picked)
                    .scheduledStart(start)
                    .scheduledEnd(end)
                    .reason("Greedy: least-loaded eligible employee")
                    .build());
        }

        return assignments;
    }
}
