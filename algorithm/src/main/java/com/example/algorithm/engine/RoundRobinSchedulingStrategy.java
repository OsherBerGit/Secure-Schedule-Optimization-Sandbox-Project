package com.example.algorithm.engine;

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
 * Each task still respects: required roles, vacation availability,
 * max task limits, and dependency constraints (predecessor finish times).
 */
public class RoundRobinSchedulingStrategy extends BaseSchedulingStrategy {

    @Override
    public String getName() {
        return "Round-Robin (Fair Distribution)";
    }

    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<TaskAssignment> assignments = new ArrayList<>();
        Map<Long, Integer> assignedCount = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();

        List<AlgoUser> allUsers = new ArrayList<>(data.users());
        int roundRobinIndex = 0;

        List<AlgoTask> sortedTasks = getSortedUnassignedTasks(data.tasks());

        for (AlgoTask task : sortedTasks) {
            LocalDateTime start = calcStartTime(task, completionTimes);
            LocalDateTime end   = calcEndTime(start, task);

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

            AlgoUser picked = pickRoundRobin(eligible, allUsers, roundRobinIndex);
            roundRobinIndex = (allUsers.indexOf(picked) + 1) % allUsers.size();

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

    private AlgoUser pickRoundRobin(List<AlgoUser> eligible,
                                     List<AlgoUser> allUsers,
                                     int currentIndex) {
        int size = allUsers.size();
        for (int i = 0; i < size; i++) {
            AlgoUser candidate = allUsers.get((currentIndex + i) % size);
            if (eligible.contains(candidate)) return candidate;
        }
        return eligible.get(0);
    }
}
