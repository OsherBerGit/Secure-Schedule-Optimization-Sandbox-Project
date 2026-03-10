package com.example.algorithm.engine.core;

import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;

import java.util.List;
import java.util.Random;

/**
 * Implements a Hill-Climbing Local Search used as the "memetic" refinement step.
 *
 * <p>After crossover and mutation produce a child, this search repeatedly attempts
 * to improve the individual by randomly re-assigning one task to a different worker.
 * If the reassignment improves fitness it is kept; otherwise the original gene is
 * restored exactly (strict hill-climbing — no simulated annealing).</p>
 *
 * <p>Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 */
public class MemeticLocalSearch {

    private final FitnessEvaluator fitnessEvaluator;
    private final Random random = new Random();

    /**
     * @param fitnessEvaluator the evaluator used to score candidate moves
     */
    public MemeticLocalSearch(FitnessEvaluator fitnessEvaluator) {
        this.fitnessEvaluator = fitnessEvaluator;
    }

    /**
     * Performs local search (hill-climbing) on a single individual in place.
     *
     * <p>Algorithm per iteration:</p>
     * <ol>
     *   <li>Select a random task slot in the chromosome.</li>
     *   <li>Select a random worker index that is different from the current assignment.</li>
     *   <li>Apply the move (update the gene).</li>
     *   <li>Evaluate the new fitness.</li>
     *   <li>If fitness improved, keep the move and update the stored fitness.
     *       Otherwise, revert the gene to its original value and restore the
     *       previous fitness — ensuring the individual is never left in a
     *       worse state than it entered.</li>
     * </ol>
     *
     * @param individual       the individual to improve (modified in place)
     * @param tasks            the ordered task list (index matches chromosome position)
     * @param users            the ordered user list (chromosome value is index into this list)
     * @param searchIterations number of improvement attempts before stopping
     */
    public void optimize(Individual individual,
                         List<AlgoTask> tasks,
                         List<AlgoUser> users,
                         int searchIterations) {

        // Ensure the initial fitness is known before we start comparing moves.
        double currentFitness = fitnessEvaluator.evaluate(individual, tasks, users);
        individual.setFitness(currentFitness);

        int numberOfTasks = tasks.size();
        int numberOfUsers = users.size();

        for (int i = 0; i < searchIterations; i++) {
            int taskIndex            = random.nextInt(numberOfTasks);
            int originalWorkerIndex  = individual.getGene(taskIndex);

            // Pick a different worker to avoid a no-op iteration.
            int newWorkerIndex = random.nextInt(numberOfUsers);
            if (newWorkerIndex == originalWorkerIndex) {
                continue;
            }

            // Apply the candidate move.
            individual.setGene(taskIndex, newWorkerIndex);

            double newFitness = fitnessEvaluator.evaluate(individual, tasks, users);

            if (newFitness > currentFitness) {
                // The move improved fitness — accept it.
                currentFitness = newFitness;
                individual.setFitness(currentFitness);
            } else {
                // The move did not improve fitness — revert to the original gene and
                // restore the known-good fitness so isFitnessCalculated() stays true.
                individual.setGene(taskIndex, originalWorkerIndex);
                individual.setFitness(currentFitness);   // re-stamp fitness after setGene cleared it
            }
        }
    }
}
