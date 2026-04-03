package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoWorkerAvailability;

import java.time.LocalTime;

/**
 * Shift Constraint — Weekly Availability Check.
 *
 * <p>Rule: The proposed task window must fall within at least ONE of the
 * worker's defined weekly availability windows for that day of the week.</p>
 *
 * <p>Specifically, for a proposed window [proposedStart, proposedEnd):
 * <ol>
 *   <li>{@code availability.dayOfWeek() == proposedStart.getDayOfWeek()}</li>
 *   <li>{@code availability.startTime() <= proposedStart.toLocalTime()}</li>
 *   <li>{@code availability.endTime()   >= proposedEnd.toLocalTime()}</li>
 * </ol>
 * At least one availability window must satisfy ALL three conditions.
 * </p>
 *
 * <p>Assumption: tasks complete on the same calendar day they start.
 * Cross-midnight tasks are outside scope and should be rejected upstream.</p>
 *
 * <p>If a worker has NO availability windows at all, the constraint passes
 * (interpreted as "always available" — e.g., admin users without shifts).
 * </p>
 *
 * <p>Zero-Trust: accesses only anonymous IDs and time values.
 * No names, emails, or PII are used in failure messages.</p>
 */
public class ShiftConstraint implements ConstraintChecker {

    @Override
    public String getName() {
        return "ShiftConstraint (Weekly Availability)";
    }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {

        // No availability windows defined → treat as always available
        if (ctx.candidate().getAvailabilities().isEmpty())
            return ConstraintResult.pass();

        LocalTime taskStart = ctx.proposedStart().toLocalTime();
        LocalTime taskEnd   = ctx.proposedEnd().toLocalTime();

        for (AlgoWorkerAvailability window : ctx.candidate().getAvailabilities()) {

            if (window.startTime() == null || window.endTime() == null) continue;

            // (a) Same day of week
            if (window.dayOfWeek() != ctx.proposedStart().getDayOfWeek()) continue;

            // (b) Shift starts at or before task start
            if (window.startTime().isAfter(taskStart)) continue;

            // (c) Shift ends at or after task end
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

