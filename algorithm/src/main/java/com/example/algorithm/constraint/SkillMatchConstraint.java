package com.example.algorithm.constraint;

import java.util.Set;

// Skill Match Constraint - Skill Verification.
// The candidate employee must possess ALL the skills required to perform the proposed task.
public class SkillMatchConstraint implements ConstraintChecker {

    @Override
    public String getName() { return "SkillMatchConstraint (Skill Match)"; }

    @Override
    public ConstraintResult check(ConstraintContext ctx) {
        Set<Long> required = ctx.task().getRequiredSkills();
        Set<Long> provided = ctx.candidate().getSkills();

        // No specific skills required -> Anyone can do it
        if (required == null || required.isEmpty())
            return ConstraintResult.pass();

        // Worker must have all required skills
        if (provided != null && provided.containsAll(required))
            return ConstraintResult.pass();

        return ConstraintResult.fail(
                "User [id=" + ctx.candidate().getId() + "] lacks required skills. " +
                        "Required: " + required + ", Provided: " + (provided == null ? "[]" : provided)
        );
    }
}
