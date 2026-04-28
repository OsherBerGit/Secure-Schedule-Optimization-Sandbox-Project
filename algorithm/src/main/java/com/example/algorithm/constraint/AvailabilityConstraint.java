package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoVacation;

import java.time.LocalDate;

// Availability Constraint.
// Vacation Overlap: The candidate employee must NOT be on an approved vacation during any part of the proposed task window.
public class AvailabilityConstraint implements ConstraintChecker {

    @Override
    public String getName() {
        return "AvailabilityConstraint (Vacation + MaxTasks)";
    }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {

        // Rule: Vacation overlap
        if (ctx.candidate().getVacations() != null) {
            LocalDate taskStart = ctx.proposedStart().toLocalDate();
            LocalDate taskEnd   = ctx.proposedEnd().toLocalDate();

            for (AlgoVacation vacation : ctx.candidate().getVacations()) {
                if (vacation.getStartDate() == null || vacation.getEndDate() == null) continue;

                boolean overlaps = !taskStart.isAfter(vacation.getEndDate())
                                && !taskEnd.isBefore(vacation.getStartDate());

                if (overlaps)
                    return ConstraintResult.fail(
                            "User [id=" + ctx.candidate().getId() + "] is on vacation ["
                            + vacation.getStartDate() + " → " + vacation.getEndDate()
                            + "] during task [id=" + ctx.task().getId() + "] window ["
                            + taskStart + " → " + taskEnd + "].");
            }
        }

        return ConstraintResult.pass();
    }
}
