package com.example.algorithm.engine.core;

import com.example.algorithm.constraint.ConstraintChecker;
import com.example.algorithm.constraint.ConstraintContext;
import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.model.AlgoSchedulingConfiguration;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates the fitness (quality) of a candidate schedule encoded as an {@link Individual}.
 *
 * <p>Scoring strategy:</p>
 * <ul>
 *   <li><b>Hard constraint violation</b> — any assignment that fails the constraint pipeline
 *       incurs a large penalty ({@code -5000} per violation) and does not contribute to the
 *       positive score.  A penalty of {@code -1000} is additionally subtracted per violation
 *       before the penalty multiplier so that the total score can never be inflated by
 *       many partial passes.</li>
 *   <li><b>Priority reward</b> — each successfully scheduled task contributes
 *       {@code priorityLevel × weightPriority}.</li>
 *   <li><b>Deadline slack reward</b> — positive hours remaining before the deadline add a small
 *       bonus: {@code slackHours × weightDeadline × 0.1}.</li>
 *   <li><b>Fairness reward</b> — low variance in per-worker task counts yields a bonus scaled by
 *       {@code weightFairness}.</li>
 *   <li>The final score is clamped to {@code ≥ 0}.</li>
 * </ul>
 *
 * <p>Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 */
public class FitnessEvaluator {

    private final List<ConstraintChecker> constraints;
    private final AlgoSchedulingConfiguration config;

    /**
     * @param constraints the same constraint pipeline used by the greedy strategies
     *                    (typically {@code PrecedenceConstraint}, {@code DeadlineConstraint},
     *                    {@code AvailabilityConstraint})
     * @param config      scheduling weights and GA parameters
     */
    public FitnessEvaluator(List<ConstraintChecker> constraints,
                             AlgoSchedulingConfiguration config) {
        this.constraints = constraints;
        this.config      = config;
    }

    /**
     * Computes the fitness score for the given individual.
     *
     * <p>The chromosome is decoded sequentially (index 0 first).  Per-worker availability
     * and per-task completion times are tracked throughout so that {@code PrecedenceConstraint}
     * and {@code AvailabilityConstraint} can be evaluated correctly for every proposed
     * assignment.</p>
     *
     * @param individual the candidate solution to evaluate
     * @param tasks      the ordered list of tasks (index {@code i} → chromosome position {@code i})
     * @param users      the ordered list of users  (chromosome value {@code j} → {@code users.get(j)})
     * @return a non-negative fitness score; higher is better
     */
    public double evaluate(Individual individual, List<AlgoTask> tasks, List<AlgoUser> users) {
        double score        = 0.0;
        int    hardViolations = 0;

        // Track how many tasks each worker has been assigned so far in this chromosome.
        Map<Long, Integer>       assignedCount   = new HashMap<>();
        // Track when each worker becomes free (for sequential scheduling within the fitness sim).
        Map<Long, LocalDateTime> workerNextFree  = new HashMap<>();
        // Track when each task finishes (needed for PrecedenceConstraint evaluation).
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        for (AlgoUser user : users) {
            assignedCount.put(user.getId(), 0);
            workerNextFree.put(user.getId(), now);
        }

        int[] chromosome = individual.getChromosome();

        for (int i = 0; i < chromosome.length; i++) {
            int workerIndex = chromosome[i];

            // A gene value of -1 (or out-of-range) means "do not assign this task".
            if (workerIndex < 0 || workerIndex >= users.size()) {
                hardViolations++;
                continue;
            }

            AlgoTask task = tasks.get(i);
            AlgoUser user = users.get(workerIndex);

            // Calculate proposed start: the later of (a) the worker's next free slot and
            // (b) the latest predecessor completion time.
            // getPredecessorTaskIds() is always non-null (returns Collections.emptyList() when absent).
            LocalDateTime proposedStart = workerNextFree.get(user.getId());
            for (Long predId : task.getPredecessorTaskIds()) {
                LocalDateTime predEnd = completionTimes.get(predId);
                if (predEnd != null && predEnd.isAfter(proposedStart)) {
                    proposedStart = predEnd;
                }
            }

            int durationHours = task.getDurationHours() != null ? task.getDurationHours() : 1;
            LocalDateTime proposedEnd = proposedStart.plusHours(durationHours);

            // Run the full constraint pipeline against this proposed assignment.
            ConstraintResult result = runConstraints(
                    task, user, proposedStart, proposedEnd, completionTimes, assignedCount);

            if (result.isValid()) {
                // Reward: priority contribution.
                score += (task.getPriorityLevel() * config.getWeightPriority());

                // Reward: deadline slack — bonus for finishing well before the deadline.
                if (task.getDeadline() != null) {
                    long slackHours = Duration.between(proposedEnd, task.getDeadline()).toHours();
                    if (slackHours > 0) {
                        score += (slackHours * config.getWeightDeadline() * 0.1);
                    }
                }

                // Advance the worker's availability and record the task's completion time.
                workerNextFree.put(user.getId(), proposedEnd);
                completionTimes.put(task.getId(), proposedEnd);
                assignedCount.merge(user.getId(), 1, Integer::sum);

            } else {
                hardViolations++;
                score -= 1000.0;
            }
        }

        // Reward: workload fairness across all workers.
        score += calculateFairnessScore(assignedCount, users.size());

        // Final score: apply the hard-violation penalty and clamp to zero.
        return Math.max(0.0, score - (hardViolations * 5000.0));
    }

    // -------------------------------------------------------------------------
    // Constraint pipeline (mirrors BaseSchedulingStrategy.runConstraints)
    // -------------------------------------------------------------------------

    /**
     * Runs every registered {@link ConstraintChecker} against the proposed assignment.
     * Returns the first failing result, or {@link ConstraintResult#pass()} if all pass.
     */
    private ConstraintResult runConstraints(AlgoTask task,
                                             AlgoUser candidate,
                                             LocalDateTime start,
                                             LocalDateTime end,
                                             Map<Long, LocalDateTime> completionTimes,
                                             Map<Long, Integer> assignedCount) {
        ConstraintContext ctx = new ConstraintContext(
                task, candidate, start, end, completionTimes, assignedCount);

        for (ConstraintChecker checker : constraints) {
            ConstraintResult result = checker.check(ctx);
            if (!result.isValid()) {
                return result;
            }
        }
        return ConstraintResult.pass();
    }

    // -------------------------------------------------------------------------
    // Fairness scoring
    // -------------------------------------------------------------------------

    /**
     * Rewards schedules where tasks are distributed evenly across workers.
     * Uses an inverse-variance formula so that lower variance yields a higher bonus.
     *
     * @param counts     map of workerId → number of tasks assigned in this evaluation
     * @param totalUsers total number of users (denominator for variance)
     * @return a non-negative fairness bonus
     */
    private double calculateFairnessScore(Map<Long, Integer> counts, int totalUsers) {
        if (totalUsers == 0) return 0.0;
        double average  = counts.values().stream().mapToInt(i -> i).average().orElse(0.0);
        double variance = counts.values().stream()
                .mapToDouble(count -> Math.pow(count - average, 2))
                .sum() / totalUsers;
        return (1.0 / (variance + 1.0)) * config.getWeightFairness() * 100.0;
    }
}

