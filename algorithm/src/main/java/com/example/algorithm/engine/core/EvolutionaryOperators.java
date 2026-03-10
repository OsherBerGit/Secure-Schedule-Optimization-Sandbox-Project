package com.example.algorithm.engine.core;

import java.util.Random;

/**
 * Provides the genetic operators used during evolution: crossover and mutation.
 *
 * <p>Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 */
public class EvolutionaryOperators {

    private final Random random = new Random();

    /**
     * Performs Uniform Crossover between two parents to produce one child.
     *
     * <p>For each gene position, the child independently inherits from either
     * parent with equal probability (50/50).  This produces high diversity and
     * disrupts building blocks less than single-point crossover when gene order
     * is not meaningful — appropriate here because chromosome positions represent
     * independent task-to-worker assignments.</p>
     *
     * @param parent1 first parent individual
     * @param parent2 second parent individual (must have the same chromosome length)
     * @return a new child individual whose fitness is not yet calculated
     */
    public Individual crossover(Individual parent1, Individual parent2) {
        int length = parent1.getChromosome().length;
        Individual child  = new Individual(length);

        for (int i = 0; i < length; i++)
            child.setGene(i, random.nextBoolean() ? parent1.getGene(i) : parent2.getGene(i));

        return child;
    }

    /**
     * Applies random mutation to an individual in place.
     *
     * <p>Each gene is independently mutated with probability {@code mutationRate}.
     * A mutated gene is reassigned to a uniformly random worker index in
     * {@code [0, numberOfWorkers)}.  Mutation never produces {@code -1}
     * (unassigned) — that is the job of the initialisation or local search
     * if required by the problem formulation.</p>
     *
     * @param individual      the individual to mutate (modified in place)
     * @param numberOfWorkers total number of available workers
     * @param mutationRate    probability [0.0, 1.0] that any given gene is mutated
     */
    public void mutate(Individual individual, int numberOfWorkers, double mutationRate) {
        for (int i = 0; i < individual.getChromosome().length; i++)
            if (random.nextDouble() < mutationRate)
                individual.setGene(i, random.nextInt(numberOfWorkers));
    }
}
