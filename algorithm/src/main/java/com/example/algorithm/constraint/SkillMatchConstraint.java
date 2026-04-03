package com.example.algorithm.constraint;

import java.util.Set;

/**
 * skill Match Constraint Ã¢â‚¬â€ Skill Verification.
 *
 * <p>Rule: The candidate employee must possess ALL the skills (skills/roles)
 * required to perform the proposed task.</p>
 *
 * <p>Specifically:
 * <ul>
 * <li>If the task requires NO specific skills (empty or null), the constraint passes (anyone can be assigned).</li>
 * <li>Otherwise, the worker's {@code skills} set must be a superset of the task's {@code requiredSkills} set.</li>
 * </ul>
 * </p>
 *
 * <p>Anti-redundancy: this mirrors the upstream filtering but acts as the explicit
 * gatekeeper inside the scheduling engine's constraint pipeline.</p>
 *
 * <p>Zero-Trust: operates solely on anonymous string identifiers representing skills.
 * No real-world skill names or human-readable text are processed.</p>
 */
public class SkillMatchConstraint implements ConstraintChecker {

    @Override
    public String getName() { return "SkillMatchConstraint (Skill Match)"; }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {
        Set<Long> required = ctx.task().getRequiredSkills();
        Set<Long> provided = ctx.candidate().getSkills();

        // Rule 1: No specific skills required -> Anyone can do it
        if (required == null || required.isEmpty())
            return ConstraintResult.pass();

        // Rule 2: Worker must have all required skills
        if (provided != null && provided.containsAll(required))
            return ConstraintResult.pass();

        return ConstraintResult.fail(
                "User [id=" + ctx.candidate().getId() + "] lacks required skills. " +
                        "Required: " + required + ", Provided: " + (provided == null ? "[]" : provided)
        );
    }
}
