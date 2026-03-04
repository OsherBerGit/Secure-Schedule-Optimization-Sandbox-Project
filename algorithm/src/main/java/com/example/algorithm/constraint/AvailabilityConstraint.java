package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoVacation;

import java.time.LocalDate;

/**
 * Availability Constraint.
 *
 * Enforces two rules in a single check:
 *
 * Rule 1 — Vacation Overlap:
 *   The candidate employee must NOT be on an approved vacation during any
 *   part of the proposed task window [proposedStart, proposedEnd].
 *   Overlap condition: taskStart <= vacationEnd AND taskEnd >= vacationStart
 *
 * Rule 2 — Max Tasks:
 *   The candidate must not have already reached their maxTasks limit
 *   for this scheduling run.
 *
 * Anti-redundancy: these rules mirror the logic in
 * {@code BaseSchedulingStrategy.isAvailableDuring()} and
 * {@code BaseSchedulingStrategy.withinTaskLimit()}, which gate the initial
 * eligible-employee list. This checker acts as the explicit, named contract
 * that runs inside the constraint pipeline and produces a descriptive reason.
 *
 * Zero-Trust: accesses only AlgoUser (anonymous ID + metadata) and
 * AlgoVacation (anonymous ID + date range). No real names used in failure reasons.
 */
public class AvailabilityConstraint implements ConstraintChecker {

    @Override
    public String getName() {
        return "AvailabilityConstraint (Vacation + MaxTasks)";
    }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {

        // ── Rule 1: Vacation overlap ─────────────────────────────────────────
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

        // ── Rule 2: Max task limit ────────────────────────────────────────────
        if (ctx.candidate().getMaxTasks() != null) {
            int currentLoad = ctx.assignedCount()
                    .getOrDefault(ctx.candidate().getId(), 0);

            if (currentLoad >= ctx.candidate().getMaxTasks())
                return ConstraintResult.fail(
                        "User [id=" + ctx.candidate().getId() + "] has reached their maxTasks limit ("
                        + ctx.candidate().getMaxTasks() + "). "
                        + "Cannot assign task [id=" + ctx.task().getId() + "].");
        }

        return ConstraintResult.pass();
    }
}

