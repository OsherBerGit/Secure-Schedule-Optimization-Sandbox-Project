package com.example.algorithm.constraint;

import java.time.LocalDateTime;

// Deadline Constraint.

// The proposed task end time must not exceed the task's hard deadline.
// If the task has no deadline set, this constraint is skipped (passes automatically).
public class DeadlineConstraint implements ConstraintChecker {

    @Override
    public String getName() {
        return "DeadlineConstraint (Hard Deadline)";
    }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {
        LocalDateTime deadline = ctx.task().getDeadline();

        if (deadline == null)
            return ConstraintResult.pass(); // no deadline set — always valid

        if (ctx.proposedEnd().isAfter(deadline))
            return ConstraintResult.fail(
                    "Task [id=" + ctx.task().getId() + "] proposed end ["
                    + ctx.proposedEnd() + "] exceeds hard deadline ["
                    + deadline + "]. Assignment rejected.");

        return ConstraintResult.pass();
    }
}

