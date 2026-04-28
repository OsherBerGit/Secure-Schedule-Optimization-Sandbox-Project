package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoWorkerAvailability;

import java.time.LocalTime;

// Shift Constraint — Weekly Availability Check.
// The proposed task window must fall within at least ONE of the worker's defined weekly availability windows for that day of the week.</p>
public class ShiftConstraint implements ConstraintChecker {

    @Override
    public String getName() { return "ShiftConstraint (Weekly Availability)"; }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {

        // No availability windows defined → treat as always available
        if (ctx.candidate().getAvailabilities().isEmpty())
            return ConstraintResult.pass();

        LocalTime taskStart = ctx.proposedStart().toLocalTime();
        LocalTime taskEnd   = ctx.proposedEnd().toLocalTime();

        for (AlgoWorkerAvailability window : ctx.candidate().getAvailabilities()) {

            if (window.startTime() == null || window.endTime() == null) continue;

            // Same day of week
            if (window.dayOfWeek() != ctx.proposedStart().getDayOfWeek()) continue;

            // Shift starts at or before task start
            if (window.startTime().isAfter(taskStart)) continue;

            // Shift ends at or after task end
            if (window.endTime().isBefore(taskEnd)) continue;

            // All conditions met — worker is on shift for the entire task window
            return ConstraintResult.pass();
        }

        return ConstraintResult.fail(
                "User [id=" + ctx.candidate().getId() + "] is not on shift during ["
                + ctx.proposedStart().toLocalTime() + " – " + ctx.proposedEnd().toLocalTime()
                + "] on " + ctx.proposedStart().getDayOfWeek()
                + ". Task [id=" + ctx.task().getId() + "] cannot be assigned.");
    }
}

