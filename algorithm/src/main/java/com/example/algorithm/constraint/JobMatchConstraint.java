package com.example.algorithm.constraint;

import java.util.Set;

/**
 * Job Match Constraint — Skill Verification.
 *
 * <p>Rule: The candidate employee must possess ALL the jobs (skills/roles)
 * required to perform the proposed task.</p>
 *
 * <p>Specifically:
 * <ul>
 * <li>If the task requires NO specific jobs (empty or null), the constraint passes (anyone can be assigned).</li>
 * <li>Otherwise, the worker's {@code jobs} set must be a superset of the task's {@code requiredJobs} set.</li>
 * </ul>
 * </p>
 *
 * <p>Anti-redundancy: this mirrors the upstream filtering but acts as the explicit
 * gatekeeper inside the scheduling engine's constraint pipeline.</p>
 *
 * <p>Zero-Trust: operates solely on anonymous string identifiers representing jobs.
 * No real-world skill names or human-readable text are processed.</p>
 */
public class JobMatchConstraint implements ConstraintChecker {

    @Override
    public String getName() { return "JobMatchConstraint (Skill Match)"; }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {
        Set<String> required = ctx.task().getRequiredJobs();
        Set<String> provided = ctx.candidate().getJobs();

        // Rule 1: No specific jobs required -> Anyone can do it
        if (required == null || required.isEmpty())
            return ConstraintResult.pass();

        // Rule 2: Worker must have all required jobs
        if (provided != null && provided.containsAll(required))
            return ConstraintResult.pass();

        return ConstraintResult.fail(
                "User [id=" + ctx.candidate().getId() + "] lacks required jobs. " +
                        "Required: " + required + ", Provided: " + (provided == null ? "[]" : provided)
        );
    }
}
