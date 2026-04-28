package com.example.algorithm.engine.core;

import java.util.Arrays;

// Represents a single candidate solution (individual) in the Memetic Algorithm population.
// The chromosome is an integer array of length numberOfTasks,
// where each index maps to a task and the value at that index is the index of the worker assigned to that task.
public class Individual {

    private final int[] chromosome;
    private double fitness;
    private boolean fitnessCalculated = false;

    // Creates a new individual with all genes initialised to unassigned.
    public Individual(int numberOfTasks) {
        this.chromosome = new int[numberOfTasks];
        Arrays.fill(this.chromosome, -1);
    }

    // Copy constructor — deep-copies the chromosome and fitness state of another individual.
    public Individual(Individual other) {
        this.chromosome = other.chromosome.clone();
        this.fitness = other.fitness;
        this.fitnessCalculated = other.fitnessCalculated;
    }

    // Assigns a worker to a task slot in the chromosome. Marks fitness as stale so it will be re-evaluated.
    public void setGene(int taskIndex, int workerIndex) {
        this.chromosome[taskIndex] = workerIndex;
        this.fitnessCalculated = false;
    }

    // Returns the worker index assigned to the given task slot.
    public int getGene(int taskIndex) {
        return this.chromosome[taskIndex];
    }

    // Returns the raw fitness score. May be stale if isFitnessCalculated() is false.
    public double getFitness() { return fitness; }

    // Stores a new fitness score and marks it as up-to-date.
    public void setFitness(double fitness) {
        this.fitness = fitness;
        this.fitnessCalculated = true;
    }

    // Returns {@code true} if the fitness score reflects the current chromosome state.
    public boolean isFitnessCalculated() { return fitnessCalculated; }

    // Returns a direct reference to the chromosome array (not a defensive copy — callers must not mutate).
    public int[] getChromosome() { return chromosome; }

    @Override
    public String toString() {
        return "Individual{fitness=" + fitness
                + ", calculated=" + fitnessCalculated
                + ", chromosome=" + Arrays.toString(chromosome) + '}';
    }
}