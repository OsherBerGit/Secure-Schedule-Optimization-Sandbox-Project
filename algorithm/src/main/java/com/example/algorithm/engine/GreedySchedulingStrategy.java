package com.example.algorithm.engine;

import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.*;

// Simple "First-Fit" Greedy scheduling strategy.
// Processes tasks in priority order (highest priority first, earliest deadline as tiebreak).
// For each task, it tries to find the first available worker who can take the task,
// looking into future shifts if necessary. The choice is based on the earliest possible start time.
public class GreedySchedulingStrategy extends BaseSchedulingStrategy {

    @Override
    public String getName() {
        return "GREEDY";
    }

    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<TaskAssignment> assignments = new ArrayList<>();
        Map<Long, Integer> assignedCount = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();
        Map<Long, TaskAssignment> assignmentsMap = new HashMap<>();
        Map<Long, LocalDateTime> workerNextFree = new HashMap<>();

        // Initialize worker next free time to now
        for (AlgoUser user : data.users())
            workerNextFree.put(user.getId(), LocalDateTime.now());

        List<AlgoTask> sortedTasks = getSortedUnassignedTasks(data.tasks());

        for (AlgoTask task : sortedTasks) {
            AlgoUser bestCandidate = null;
            LocalDateTime bestStartTime = null;
            ConstraintResult lastFail = null;

            // Iterate through all users to find the one who can start the task earliest.
            for (AlgoUser candidate : data.users()) {
                // Find the earliest this candidate can start the task, considering their shifts and previous tasks.
                Optional<LocalDateTime> possibleStartOpt = findNextAvailableStartTime(task, candidate, assignmentsMap, workerNextFree.get(candidate.getId()));

                if (possibleStartOpt.isPresent()) {
                    LocalDateTime possibleStart = possibleStartOpt.get();
                    
                    // If this candidate can start earlier than the best one found so far, evaluate them.
                    if (bestStartTime == null || possibleStart.isBefore(bestStartTime)) {
                        LocalDateTime possibleEnd = calcEndTime(possibleStart, task);
                        ConstraintResult result = validateHardConstraints(task, candidate, possibleStart, possibleEnd, completionTimes, assignedCount, assignments);

                        if (result.isValid()) {
                            // We found a new best candidate.
                            bestCandidate = candidate;
                            bestStartTime = possibleStart;
                        } else
                            lastFail = result;
                    }
                } else
                     if (lastFail == null)
                        lastFail = ConstraintResult.fail("Could not find a valid shift window for user [id=" + candidate.getId() + "] within the next 7 days.");
            }

            // Handle assignment result
            if (bestCandidate != null) {
                // Assign the task to the best candidate found.
                LocalDateTime chosenEnd = calcEndTime(bestStartTime, task);
                assignedCount.merge(bestCandidate.getId(), 1, Integer::sum);
                completionTimes.put(task.getId(), chosenEnd);
                workerNextFree.put(bestCandidate.getId(), chosenEnd); // Update when this worker is next free.

                TaskAssignment newAssignment = TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(bestCandidate)
                        .scheduledStart(bestStartTime)
                        .scheduledEnd(chosenEnd)
                        .reason("Greedy: Earliest available worker")
                        .build();

                assignments.add(newAssignment);
                assignmentsMap.put(task.getId(), newAssignment);
            } else {
                // No suitable worker was found for this task.
                String reason = lastFail != null
                        ? "Constraint violation: " + lastFail.getReason()
                        : "No available or eligible workers found.";
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason(reason)
                        .build());
            }
        }

        return assignments;
    }
}
