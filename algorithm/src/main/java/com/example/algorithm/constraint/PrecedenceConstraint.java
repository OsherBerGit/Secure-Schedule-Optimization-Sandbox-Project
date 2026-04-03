package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoConstraint;
import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FINISH_TO_START Precedence Constraint.
 *
 * Rule: Task B may not start before ALL of its predecessor tasks have finished.
 *
 * This constraint checks whether the {@code proposedStart} in the context
 * is strictly after (or equal to) the completion time of every predecessor
 * listed in {@code task.getPredecessorTaskIds()}.
 *
 * If a predecessor has not yet been scheduled (its end time is absent from
 * {@code completionTimes}), the assignment is rejected — unresolved
 * dependencies are treated as blocking.
 *
 * Anti-redundancy: the actual time calculation reuses
 * {@code BaseSchedulingStrategy.calcStartTime()}, which already computes
 * the earliest valid start from completionTimes. This checker acts as the
 * explicit gate that enforces that contract and surfaces a descriptive reason.
 *
 * Zero-Trust: operates solely on anonymous IDs and timestamps from AlgoTask.
 */
public class PrecedenceConstraint implements ConstraintChecker {

    @Override
    public String getName() {
        return "PrecedenceConstraint (FS, SS, FF, SF)";
    }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {
        if (ctx.task().getConstraints() == null || ctx.task().getConstraints().isEmpty())
            return ConstraintResult.pass(); // no dependencies — always valid

        Map<Long, TaskAssignment> scheduledTasks = ctx.currentAssignments().stream()
                .collect(Collectors.toMap(
                        a -> a.getTask().getId(),
                        a -> a,
                        (a, b) -> a
                ));

        for (AlgoConstraint constraint : ctx.task().getConstraints()) {
            Long predId = constraint.predecessorId();
            TaskAssignment predAssignment = scheduledTasks.get(predId);

            // Predecessor has not been scheduled yet — treat as blocking
            if (predAssignment == null)
                return ConstraintResult.fail(
                        "Predecessor task [id=" + predId + "] has not been scheduled yet. "
                                + "Task [id=" + ctx.task().getId() + "] cannot be evaluated before it.");

            LocalDateTime predStart = predAssignment.getScheduledStart();
            LocalDateTime predEnd   = predAssignment.getScheduledEnd();

            if (predStart == null || predEnd == null)
                return ConstraintResult.fail("Predecessor task [id=" + predId + "] scheduled times are null.");

            LocalDateTime proposedStart = ctx.proposedStart();
            LocalDateTime proposedEnd   = ctx.proposedEnd();

            switch (constraint.type()) {
                case FS -> {
                    if (proposedStart.isBefore(predEnd))
                        return ConstraintResult.fail("FS Violated: Task [" + ctx.task().getId() +
                                "] proposed start (" + proposedStart + ") is before predecessor [" +
                                predId + "] end (" + predEnd + ").");
                }
                case SS -> {
                    if (proposedStart.isBefore(predStart))
                        return ConstraintResult.fail("SS Violated: Task [" + ctx.task().getId() +
                                "] proposed start (" + proposedStart + ") is before predecessor [" +
                                predId + "] start (" + predStart + ").");
                }
                case FF -> {
                    if (proposedEnd.isBefore(predEnd))
                        return ConstraintResult.fail("FF Violated: Task [" + ctx.task().getId() +
                                "] proposed end (" + proposedEnd + ") is before predecessor [" +
                                predId + "] end (" + predEnd + ").");
                }
                case SF -> {
                    if (proposedEnd.isBefore(predStart))
                        return ConstraintResult.fail("SF Violated: Task [" + ctx.task().getId() +
                                "] proposed end (" + proposedEnd + ") is before predecessor [" +
                                predId + "] start (" + predStart + ").");
                }
            }
        }

        return ConstraintResult.pass();
    }
}

