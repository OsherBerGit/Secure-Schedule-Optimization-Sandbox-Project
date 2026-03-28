package com.example.algorithm.engine;

import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Simple Round-Robin Strategy.
 *
 * <p>Distributes tasks evenly across eligible employees by cycling through them in order.
 * This strategy now uses the forward-looking `findNextAvailableStartTime` method to
 * ensure tasks are scheduled in a worker's future shifts if they are not available 'right now'.
 * </p>
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
        Map<Long, LocalDateTime> workerNextFree = new HashMap<>();

        List<AlgoUser> allUsers = new ArrayList<>(data.users());
        int roundRobinIndex = 0;

        // Initialize worker next free time to now
        for (AlgoUser user : allUsers)
            workerNextFree.put(user.getId(), LocalDateTime.now());

        List<AlgoTask> sortedTasks = getSortedUnassignedTasks(data.tasks());

        for (AlgoTask task : sortedTasks) {
            AlgoUser picked = null;
            ConstraintResult lastFail = null;
            LocalDateTime chosenStart = null;
            LocalDateTime chosenEnd = null;
            int size = allUsers.size();

            if (size == 0) {
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason("No candidate employees found")
                        .build());
                continue;
            }

            // Iterate through workers in round-robin order to find the first valid assignment.
            for (int i = 0; i < size; i++) {
                AlgoUser candidate = allUsers.get((roundRobinIndex + i) % size);

                // Find the earliest this candidate can start, considering their personal timeline.
                Optional<LocalDateTime> possibleStartOpt = findNextAvailableStartTime(task, candidate, completionTimes, workerNextFree.get(candidate.getId()));

                if (possibleStartOpt.isPresent()) {
                    LocalDateTime start = possibleStartOpt.get();
                    LocalDateTime end = calcEndTime(start, task);

                    ConstraintResult result = validateHardConstraints(
                            task, candidate, start, end, completionTimes, assignedCount, assignments);

                    if (result.isValid()) {
                        picked = candidate;
                        chosenStart = start;
                        chosenEnd = end;
                        // Advance round-robin index past this successful pick.
                        roundRobinIndex = (roundRobinIndex + i + 1) % size;
                        break; // Found a valid assignment, move to the next task.
                    }
                    lastFail = result;
                } else
                    lastFail = ConstraintResult.fail("Could not find a valid shift window for user [id=" + candidate.getId() + "] within the next 7 days.");
            }

            if (picked != null) {
                // Task was successfully assigned.
                assignedCount.merge(picked.getId(), 1, Integer::sum);
                completionTimes.put(task.getId(), chosenEnd);
                workerNextFree.put(picked.getId(), chosenEnd); // Update the worker's timeline.

                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(picked)
                        .scheduledStart(chosenStart)
                        .scheduledEnd(chosenEnd)
                        .reason(String.format(
                                "Round-Robin: turn-based pick (now has %d task(s))",
                                assignedCount.get(picked.getId())))
                        .build());
            } else {
                // No worker could be found for this task in the round-robin cycle.
                String reason = lastFail != null
                        ? "Constraint violation: " + lastFail.getReason()
                        : "All candidates failed constraint checks.";
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason(reason)
                        .build());
                
                // Still advance the index so the next task tries the next worker in the rotation.
                if (size > 0)
                    roundRobinIndex = (roundRobinIndex + 1) % size;
            }
        }

        return assignments;
    }
}
