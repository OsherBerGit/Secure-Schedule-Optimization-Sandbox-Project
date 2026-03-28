package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of all data a {@link ConstraintChecker} needs to evaluate one
 * candidate (user → task) assignment.
 *
 * Zero-Trust compliant: contains only anonymous algorithm models (IDs + scheduling
 * metadata). No real names, no DB references, no entity objects.
 *
 * @param task             the task being considered for assignment
 * @param candidate        the employee being evaluated
 * @param proposedStart    calculated start time for this task
 * @param proposedEnd      calculated end time (start + durationHours)
 * @param completionTimes  map of taskId → computed end time for already-scheduled tasks
 *                         (used to evaluate predecessor constraints)
 * @param assignedCount    map of userId → number of tasks already assigned in this run
 *                         (used to evaluate maxTasks constraints)
 * @param currentAssignments List of all {@link TaskAssignment} results produced so far
 *                          in this session (used to evaluate overlaps).
 */
public record ConstraintContext(
        AlgoTask               task,
        AlgoUser               candidate,
        LocalDateTime          proposedStart,
        LocalDateTime          proposedEnd,
        Map<Long, LocalDateTime> completionTimes,
        Map<Long, Integer>       assignedCount,
        List<TaskAssignment> currentAssignments
) {}

