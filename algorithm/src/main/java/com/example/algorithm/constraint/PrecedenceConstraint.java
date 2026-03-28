package com.example.algorithm.constraint;

import java.time.LocalDateTime;

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
        return "PrecedenceConstraint (FINISH_TO_START)";
    }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {
        if (ctx.task().getPredecessorTaskIds() == null || ctx.task().getPredecessorTaskIds().isEmpty())
            return ConstraintResult.pass(); // no dependencies — always valid

        for (Long predecessorId : ctx.task().getPredecessorTaskIds()) {

            LocalDateTime predecessorEnd = ctx.completionTimes().get(predecessorId);

            // Predecessor has not been scheduled yet — treat as blocking
            if (predecessorEnd == null)
                return ConstraintResult.fail(
                        "Predecessor task [id=" + predecessorId + "] has not been scheduled yet. "
                        + "Task [id=" + ctx.task().getId() + "] cannot start before it finishes.");

            // Proposed start must be at or after the predecessor's end
            if (ctx.proposedStart().isBefore(predecessorEnd))
                return ConstraintResult.fail(
                        "Task [id=" + ctx.task().getId() + "] proposed start ["
                        + ctx.proposedStart() + "] is before predecessor task [id="
                        + predecessorId + "] end [" + predecessorEnd + "]. "
                        + "FINISH_TO_START constraint violated.");
        }

        return ConstraintResult.pass();
    }
}

