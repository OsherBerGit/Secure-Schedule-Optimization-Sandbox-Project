package com.example.algorithm.engine.core;

import com.example.algorithm.constraint.ConstraintChecker;
import com.example.algorithm.constraint.ConstraintContext;
import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.constraint.Scorer;
import com.example.algorithm.model.AlgoSchedulingConfiguration;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluates the fitness (quality) of a candidate schedule encoded as an {@link Individual}.
 *
 * <p>Scoring strategy:</p>
 * <ul>
 *   <li><b>Base Score:</b> Each successfully scheduled task contributes a large positive score (e.g., +1000).
 *       This is the primary driver of fitness.</li>
 *   <li><b>Hard Constraint Violation:</b> An assignment that fails the constraint pipeline does not get the base score
 *       and incurs a moderate penalty. This ensures invalid assignments are heavily disfavored.</li>
 *   <li><b>Soft Constraints (Bonuses/Penalties):</b>
 *     <ul>
 *       <li><b>Priority:</b> Higher priority tasks add a bonus scaled by {@code weightPriority}.</li>
 *       <li><b>Deadline Slack:</b> Finishing a task well before its deadline adds a bonus.</li>
 *       <li><b>Fairness:</b> Evenly distributed workloads across users add a bonus.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class FitnessEvaluator {

    private final List<ConstraintChecker> hardConstraints;
    private final List<Scorer> softScorers;
    private final AlgoSchedulingConfiguration config;

    private static final double BASE_SCORE_PER_TASK = 1000.0;
    private static final double HARD_VIOLATION_PENALTY = 500.0;

    public FitnessEvaluator(List<ConstraintChecker> hardConstraints,
                            List<Scorer> softScorers,
                            AlgoSchedulingConfiguration config) {
        this.hardConstraints = hardConstraints;
        this.softScorers = softScorers;
        this.config = config;
    }

    public double evaluate(Individual individual, List<AlgoTask> tasks, List<AlgoUser> users) {
        double score = 0.0;
        Map<Long, Integer> assignedCount = new HashMap<>();
        Map<Long, LocalDateTime> workerNextFree = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();
        List<TaskAssignment> currentAssignments = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        for (AlgoUser user : users) {
            assignedCount.put(user.getId(), 0);
            workerNextFree.put(user.getId(), now);
        }

        int[] chromosome = individual.getChromosome();
        for (int i = 0; i < chromosome.length; i++) {
            int workerIndex = chromosome[i];
            if (workerIndex < 0 || workerIndex >= users.size()) continue; // Unassigned tasks don't get a positive score, but we don't penalize them here.

            AlgoTask task = tasks.get(i);
            AlgoUser user = users.get(workerIndex);

            // Use the exact same logic as BaseSchedulingStrategy to find the start time
            Optional<LocalDateTime> possibleStart = findNextAvailableStartTimeForEvaluation(task, user, completionTimes, workerNextFree.get(user.getId()));

            if (possibleStart.isPresent()) {
                LocalDateTime proposedStart = possibleStart.get();
                int durationHours = task.getDurationHours() != null ? task.getDurationHours() : 1;
                LocalDateTime proposedEnd = proposedStart.plusHours(durationHours);

                ConstraintContext ctx = new ConstraintContext(
                        task, user, proposedStart, proposedEnd, completionTimes, assignedCount, currentAssignments);

                if (runHardConstraints(ctx).isValid()) {
                    score += BASE_SCORE_PER_TASK; // Primary reward for a valid assignment

                    for (Scorer scorer : softScorers)
                        score += scorer.score(ctx, config);

                    if (task.getDeadline() != null) {
                        long slackHours = Duration.between(proposedEnd, task.getDeadline()).toHours();
                        if (slackHours > 0)
                            score += (slackHours * config.getWeightDeadline() * 0.1);
                    }

                    workerNextFree.put(user.getId(), proposedEnd);
                    completionTimes.put(task.getId(), proposedEnd);
                    assignedCount.merge(user.getId(), 1, Integer::sum);
                    currentAssignments.add(TaskAssignment.builder().task(task).assignedEmployee(user).scheduledStart(proposedStart).scheduledEnd(proposedEnd).build());
                } else
                    score -= HARD_VIOLATION_PENALTY; // Penalize invalid assignments
            } else
                 score -= HARD_VIOLATION_PENALTY; // Penalize if no shift can be found
        }

        score += calculateFairnessScore(assignedCount, users.size());
        return Math.max(0.0, score);
    }

    // --- Helper logic duplicated from BaseSchedulingStrategy to ensure exact 1:1 evaluation ---

    private Optional<LocalDateTime> findNextAvailableStartTimeForEvaluation(AlgoTask task, AlgoUser worker,
                                                                 Map<Long, LocalDateTime> completionTimes,
                                                                 LocalDateTime workerNextFree) {
        LocalDateTime earliestPossible = LocalDateTime.now();
        if (task.getPredecessorTaskIds() != null) {
            for (Long predId : task.getPredecessorTaskIds()) {
                LocalDateTime predEnd = completionTimes.get(predId);
                if (predEnd != null && predEnd.isAfter(earliestPossible))
                    earliestPossible = predEnd;
            }
        }

        if (workerNextFree != null && workerNextFree.isAfter(earliestPossible))
            earliestPossible = workerNextFree;

        int taskDurationHours = task.getDurationHours() != null ? task.getDurationHours() : 1;

        if (worker.getAvailabilities().isEmpty())
            return Optional.of(earliestPossible);

        List<com.example.algorithm.model.AlgoWorkerAvailability> sortedAvailabilities = worker.getAvailabilities().stream()
                .sorted(Comparator.comparing(com.example.algorithm.model.AlgoWorkerAvailability::dayOfWeek).thenComparing(com.example.algorithm.model.AlgoWorkerAvailability::startTime))
                .toList();

        for (int i = 0; i < 7; i++) {
            LocalDateTime dayToSearch = earliestPossible.plusDays(i);
            DayOfWeek currentDay = dayToSearch.getDayOfWeek();

            for (com.example.algorithm.model.AlgoWorkerAvailability shift : sortedAvailabilities) {
                if (shift.dayOfWeek() == currentDay) {
                    LocalDateTime shiftStart = dayToSearch.toLocalDate().atTime(shift.startTime());
                    LocalDateTime shiftEnd = dayToSearch.toLocalDate().atTime(shift.endTime());

                    LocalDateTime actualStart = (shiftStart.isAfter(earliestPossible)) ? shiftStart : earliestPossible;
                    
                    if (i > 0)
                        actualStart = shiftStart;

                    if (actualStart.isBefore(shiftEnd) && (actualStart.plusHours(taskDurationHours).isBefore(shiftEnd) || actualStart.plusHours(taskDurationHours).isEqual(shiftEnd)))
                        return Optional.of(actualStart);
                }
            }
        }
        return Optional.empty();
    }


    private ConstraintResult runHardConstraints(ConstraintContext ctx) {
        for (ConstraintChecker checker : hardConstraints) {
            ConstraintResult result = checker.check(ctx);
            if (!result.isValid())
                return result;
        }
        return ConstraintResult.pass();
    }

    private double calculateFairnessScore(Map<Long, Integer> counts, int totalUsers) {
        if (totalUsers == 0) return 0.0;
        double average = counts.values().stream().mapToInt(i -> i).average().orElse(0.0);
        double variance = counts.values().stream()
                .mapToDouble(count -> Math.pow(count - average, 2))
                .sum() / totalUsers;
        return (1.0 / (variance + 1.0)) * config.getWeightFairness() * 100.0;
    }
}
