package com.example.algorithm.constraint;

import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Overlap Constraint — Temporal Collision Avoidance.
 *
 * <p>Rule: A candidate employee must NOT have any other task assigned during
 * the proposed time window [proposedStart, proposedEnd].</p>
 *
 * <p>Logic:
 * The checker iterates through all tasks already assigned to this user in the
 * current scheduling session. An overlap occurs if:
 * {@code (proposedStart < existingEnd) AND (proposedEnd > existingStart)}
 * </p>
 *
 * <p>Zero-Trust: Accesses only anonymous task IDs and time intervals.
 * No information about the nature of the overlapping tasks is exposed.</p>
 */
public class OverlapConstraint implements ConstraintChecker {

    @Override
    public String getName() {
        return "OverlapConstraint (No Simultaneous Tasks)";
    }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {
        // We need to check all tasks ALREADY assigned to THIS candidate in this run
        List<TaskAssignment> existingAssignments = ctx.currentAssignments();

        if (existingAssignments == null || existingAssignments.isEmpty())
            return ConstraintResult.pass();

        LocalDateTime newStart = ctx.proposedStart();
        LocalDateTime newEnd   = ctx.proposedEnd();

        for (TaskAssignment existing : existingAssignments) {
            // Check only assignments for the SAME employee
            if (existing.getAssignedEmployee() == null || !existing.getAssignedEmployee().getId().equals(ctx.candidate().getId())) continue;

            // Standard Overlap Formula: (StartA < EndB) AND (EndA > StartB)
            boolean overlaps = newStart.isBefore(existing.getScheduledEnd()) && newEnd.isAfter(existing.getScheduledStart());

            if (overlaps)
                return ConstraintResult.fail(
                        "User [id=" + ctx.candidate().getId() + "] is already busy with Task [id="
                                + existing.getTask().getId() + "] from " + existing.getScheduledStart()
                                + " to " + existing.getScheduledEnd()
                );
        }

        return ConstraintResult.pass();
    }
}
