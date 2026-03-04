package com.example.algorithm.engine;

import com.example.algorithm.constraint.AvailabilityConstraint;
import com.example.algorithm.constraint.ConstraintChecker;
import com.example.algorithm.constraint.ConstraintContext;
import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.constraint.PrecedenceConstraint;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.AlgoVacation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Shared helper utilities used by all scheduling strategy implementations.
 * Handles eligibility checks, vacation overlap, workload tracking, start time
 * calculation, and the modular constraint-checking pipeline.
 */
abstract class BaseSchedulingStrategy implements SchedulingStrategy {

    /**
     * The active constraint pipeline.
     * All concrete strategies inherit and run these two core constraints.
     * Additional constraints can be added here without touching strategy code.
     */
    protected final List<ConstraintChecker> constraints = List.of(
            new PrecedenceConstraint(),
            new AvailabilityConstraint()
    );

    // -------------------------------------------------------------------------
    // Eligibility
    // -------------------------------------------------------------------------

    /**
     * Returns true if the employee has at least one of the required roles for the task.
     * If the task requires no roles, any employee qualifies.
     */
    protected boolean hasRequiredRole(AlgoUser user, AlgoTask task) {
        if (task.getRequiredRoles() == null || task.getRequiredRoles().isEmpty()) return true;
        for (String required : task.getRequiredRoles())
            if (user.getRoles().contains(required)) return true;
        return false;
    }

    /**
     * Returns true if the employee is NOT on an approved vacation during the task window.
     */
    protected boolean isAvailableDuring(AlgoUser user, LocalDateTime start, LocalDateTime end) {
        if (user.getVacations() == null) return true;
        LocalDate taskStart = start.toLocalDate();
        LocalDate taskEnd   = end.toLocalDate();
        for (AlgoVacation v : user.getVacations())
            // overlap: task starts before vacation ends AND task ends after vacation starts
            if (!taskStart.isAfter(v.getEndDate()) && !taskEnd.isBefore(v.getStartDate()))
                return false;
        return true;
    }

    /**
     * Returns true if the employee has not exceeded their maxTasks limit.
     * @param assignedCount how many tasks are already assigned to this employee in this run
     */
    protected boolean withinTaskLimit(AlgoUser user, Map<Long, Integer> assignedCount) {
        if (user.getMaxTasks() == null) return true;
        int current = assignedCount.getOrDefault(user.getId(), 0);
        return current < user.getMaxTasks();
    }

    // -------------------------------------------------------------------------
    // Start time calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates the earliest start time for a task.
     * Respects predecessor task end times and defaults to now if no dependencies.
     *
     * @param task        the task to schedule
     * @param assignments map of taskId -> already-computed TaskAssignment (for dependency lookup)
     */
    protected LocalDateTime calcStartTime(AlgoTask task, Map<Long, LocalDateTime> completionTimes) {
        LocalDateTime earliest = LocalDateTime.now();
        if (task.getPredecessorTaskIds() != null)
            for (Long predId : task.getPredecessorTaskIds()) {
                LocalDateTime predEnd = completionTimes.get(predId);
                if (predEnd != null && predEnd.isAfter(earliest)) earliest = predEnd;
            }
        return earliest;
    }

    /**
     * Calculates the end time of a task given its start time and duration.
     */
    protected LocalDateTime calcEndTime(LocalDateTime start, AlgoTask task) {
        int hours = task.getDurationHours() != null ? task.getDurationHours() : 1;
        return start.plusHours(hours);
    }

    // -------------------------------------------------------------------------
    // Sorting
    // -------------------------------------------------------------------------

    /**
     * Sorts tasks by priority level descending (CRITICAL first), then by deadline ascending.
     * Only returns tasks that are PENDING or IN_PROGRESS (skips COMPLETED/CANCELLED).
     */
    protected List<AlgoTask> getSortedUnassignedTasks(List<AlgoTask> tasks) {
        List<AlgoTask> result = new ArrayList<>();
        for (AlgoTask t : tasks) {
            String status = t.getStatus();
            if (status != null && (status.equalsIgnoreCase("COMPLETED") || status.equalsIgnoreCase("CANCELLED")))
                continue;
            result.add(t);
        }
        result.sort((a, b) -> {
            int pa = a.getPriorityLevel() != null ? a.getPriorityLevel() : 0;
            int pb = b.getPriorityLevel() != null ? b.getPriorityLevel() : 0;
            if (pb != pa) return Integer.compare(pb, pa); // higher level first
            if (a.getDeadline() != null && b.getDeadline() != null)
                return a.getDeadline().compareTo(b.getDeadline()); // earlier deadline first
            return 0;
        });
        return result;
    }

    /**
     * Filters employees to only those who are eligible for the given task
     * (correct role, within task limit, not on vacation during the task window).
     */
    protected List<AlgoUser> getEligibleEmployees(AlgoTask task, List<AlgoUser> users,
                                                   Map<Long, Integer> assignedCount,
                                                   LocalDateTime start, LocalDateTime end) {
        List<AlgoUser> eligible = new ArrayList<>();
        for (AlgoUser u : users)
            if (hasRequiredRole(u, task)
                    && withinTaskLimit(u, assignedCount)
                    && isAvailableDuring(u, start, end))
                eligible.add(u);
        return eligible;
    }

    // -------------------------------------------------------------------------
    // Constraint pipeline
    // -------------------------------------------------------------------------

    /**
     * Runs every registered {@link ConstraintChecker} against the proposed
     * (task → candidate) assignment.
     *
     * Returns the first failing {@link ConstraintResult}, or a passing result
     * if all constraints are satisfied.
     *
     * Strategies call this AFTER {@link #getEligibleEmployees} as a second,
     * explicit validation gate — defence-in-depth for assignment correctness.
     *
     * @param task            the task being assigned
     * @param candidate       the employee being evaluated
     * @param start           proposed start time
     * @param end             proposed end time
     * @param completionTimes map of taskId → end time for already-scheduled tasks
     * @param assignedCount   map of userId → tasks assigned so far in this run
     * @return first failing result, or {@link ConstraintResult#pass()} if all pass
     */
    protected ConstraintResult runConstraints(AlgoTask task,
                                              AlgoUser candidate,
                                              LocalDateTime start,
                                              LocalDateTime end,
                                              Map<Long, LocalDateTime> completionTimes,
                                              Map<Long, Integer> assignedCount) {
        ConstraintContext ctx = new ConstraintContext(
                task, candidate, start, end, completionTimes, assignedCount);

        for (ConstraintChecker checker : constraints) {
            ConstraintResult result = checker.check(ctx);
            if (!result.isValid())
                return result; // fail-fast on first violated constraint
        }
        return ConstraintResult.pass();
    }
}

