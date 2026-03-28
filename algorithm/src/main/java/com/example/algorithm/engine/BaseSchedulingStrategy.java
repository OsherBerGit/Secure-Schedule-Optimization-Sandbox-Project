package com.example.algorithm.engine;

import com.example.algorithm.constraint.*;
import com.example.algorithm.model.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared helper utilities used by all scheduling strategy implementations.
 * Handles start time calculation, sorting, and the modular constraint-checking pipeline.
 */
public abstract class BaseSchedulingStrategy implements SchedulingStrategy {

    protected final List<ConstraintChecker> hardConstraints = List.of(
            new JobMatchConstraint(),
            new OverlapConstraint(),
            new PrecedenceConstraint(),
            new DeadlineConstraint(),
            new AvailabilityConstraint(),
            new ShiftConstraint()
    );

    protected final List<Scorer> softScorers = List.of(
            new MaxTasksScorer(),
            new PriorityScorer()
    );

    protected LocalDateTime calcStartTime(AlgoTask task, Map<Long, LocalDateTime> completionTimes) {
        LocalDateTime earliest = LocalDateTime.now();
        if (task.getPredecessorTaskIds() != null) {
            for (Long predId : task.getPredecessorTaskIds()) {
                LocalDateTime predEnd = completionTimes.get(predId);
                if (predEnd != null && predEnd.isAfter(earliest)) {
                    earliest = predEnd;
                }
            }
        }
        return earliest;
    }

    protected LocalDateTime calcEndTime(LocalDateTime start, AlgoTask task) {
        int hours = task.getDurationHours() != null ? task.getDurationHours() : 1;
        return start.plusHours(hours);
    }

    /**
     * Finds the earliest possible start time for a task by a specific worker,
     * considering dependencies, the worker's previous task completion, and shift availability.
     *
     * @param task            The task to schedule.
     * @param worker          The worker being considered.
     * @param completionTimes Map of already completed task IDs to their end times.
     * @param workerNextFree  The time the worker finishes their previous assignment.
     * @return An Optional containing the earliest valid start time, or empty if no valid slot is found within 7 days.
     */
    protected Optional<LocalDateTime> findNextAvailableStartTime(AlgoTask task, AlgoUser worker,
                                                                 Map<Long, LocalDateTime> completionTimes,
                                                                 LocalDateTime workerNextFree) {
        LocalDateTime earliestPossible = calcStartTime(task, completionTimes);
        
        if (workerNextFree != null && workerNextFree.isAfter(earliestPossible))
            earliestPossible = workerNextFree;

        int taskDurationHours = task.getDurationHours() != null ? task.getDurationHours() : 1;

        if (worker.getAvailabilities().isEmpty())
            return Optional.of(earliestPossible);

        List<AlgoWorkerAvailability> sortedAvailabilities = worker.getAvailabilities().stream()
                .sorted(Comparator.comparing(AlgoWorkerAvailability::dayOfWeek).thenComparing(AlgoWorkerAvailability::startTime))
                .toList();

        for (int i = 0; i < 7; i++) {
            LocalDateTime dayToSearch = earliestPossible.plusDays(i);
            DayOfWeek currentDay = dayToSearch.getDayOfWeek();

            for (AlgoWorkerAvailability shift : sortedAvailabilities) {
                if (shift.dayOfWeek() == currentDay) {
                    LocalDateTime shiftStart = dayToSearch.toLocalDate().atTime(shift.startTime());
                    LocalDateTime shiftEnd = dayToSearch.toLocalDate().atTime(shift.endTime());

                    // The actual start time must be after the shift starts AND after the earliest possible time.
                    LocalDateTime actualStart = (shiftStart.isAfter(earliestPossible)) ? shiftStart : earliestPossible;
                    
                    // If the calculated start is on a future day, we must align it with the shift start time, not now().
                    if (i > 0)
                        actualStart = shiftStart;

                    if (actualStart.isBefore(shiftEnd) && (actualStart.plusHours(taskDurationHours).isBefore(shiftEnd) || actualStart.plusHours(taskDurationHours).isEqual(shiftEnd)))
                        return Optional.of(actualStart);
                }
            }
        }

        return Optional.empty();
    }

    protected List<AlgoTask> getSortedUnassignedTasks(List<AlgoTask> tasks) {
        return tasks.stream()
                .filter(this::isTaskPending)
                .sorted(Comparator
                        .comparingInt((AlgoTask t) -> t.getPriorityLevel() != null ? t.getPriorityLevel() : 0)
                        .reversed()
                        .thenComparing(AlgoTask::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private boolean isTaskPending(AlgoTask t) {
        String status = t.getStatus();
        if (status == null) return true;
        return !status.equalsIgnoreCase("COMPLETED") && !status.equalsIgnoreCase("CANCELLED");
    }

    protected ConstraintResult validateHardConstraints(AlgoTask task,
                                                       AlgoUser candidate,
                                                       LocalDateTime start,
                                                       LocalDateTime end,
                                                       Map<Long, LocalDateTime> completionTimes,
                                                       Map<Long, Integer> assignedCount,
                                                       List<TaskAssignment> currentAssignments) {
        ConstraintContext ctx = new ConstraintContext(
                task, candidate, start, end, completionTimes, assignedCount, currentAssignments);

        for (ConstraintChecker checker : hardConstraints) {
            ConstraintResult result = checker.check(ctx);
            if (!result.isValid())
                return result;
        }
        return ConstraintResult.pass();
    }
}
