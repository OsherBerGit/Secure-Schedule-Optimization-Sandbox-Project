package com.example.algorithm.engine.core;

import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;

import java.util.List;
import java.util.Random;

// Implements a Hill-Climbing Local Search used as the "memetic" refinement step.
// Also acts as a "Repair" mechanism for completely invalid genes.
public class LocalSearch {

    private final FitnessEvaluator fitnessEvaluator;
    private final Random random = new Random();

    public LocalSearch(FitnessEvaluator fitnessEvaluator) {
        this.fitnessEvaluator = fitnessEvaluator;
    }

    public void optimize(Individual individual, List<AlgoTask> tasks, List<AlgoUser> users, int searchIterations) {

        // 1. Repair Phase: If a gene is invalid (e.g., causes a hard penalty), try to fix it.
        // We know a gene is invalid if the overall fitness is very low, but since fitness
        // is evaluated cumulatively, we'll use a heuristic: try replacing random genes
        // to see if we get a massive jump in fitness (indicating a repaired constraint).
        double currentFitness = fitnessEvaluator.evaluate(individual, tasks, users);
        individual.setFitness(currentFitness);

        int numberOfTasks = tasks.size();
        int numberOfUsers = users.size();

        // If fitness is extremely low, it means we have hard constraint violations.
        // Let's dedicate some iterations to purely trying to escape this "invalid" valley.
        int repairAttempts = currentFitness < 1000 ? numberOfTasks : 0; // If score is < 1 valid task, try repairing all

        for (int i = 0; i < repairAttempts; i++) {
            int taskIndex = i; // Systematically try to fix each task
            int originalWorkerIndex = individual.getGene(taskIndex);

            // Try assigning to "unassigned" (-1) first as a safe fallback
            individual.setGene(taskIndex, -1);
            double unassignedFitness = fitnessEvaluator.evaluate(individual, tasks, users);
            
            double bestRepairFitness = unassignedFitness;
            int bestRepairWorker = -1;

            // Then try other workers
            for (int w = 0; w < numberOfUsers; w++) {
                if (w == originalWorkerIndex) continue;
                individual.setGene(taskIndex, w);
                double f = fitnessEvaluator.evaluate(individual, tasks, users);
                if (f > bestRepairFitness) {
                    bestRepairFitness = f;
                    bestRepairWorker = w;
                }
            }

            // If we found a better state (even if it's just leaving it unassigned), keep it
            if (bestRepairFitness > currentFitness) {
                currentFitness = bestRepairFitness;
                individual.setGene(taskIndex, bestRepairWorker);
                individual.setFitness(currentFitness);
            } else
                // Revert
                individual.setGene(taskIndex, originalWorkerIndex);
        }


        // 2. Optimization Phase: Standard Hill-Climbing
        for (int i = 0; i < searchIterations; i++) {
            int taskIndex = random.nextInt(numberOfTasks);
            int originalWorkerIndex = individual.getGene(taskIndex);

            int newWorkerIndex = random.nextInt(numberOfUsers);
            if (newWorkerIndex == originalWorkerIndex) continue;

            individual.setGene(taskIndex, newWorkerIndex);
            double newFitness = fitnessEvaluator.evaluate(individual, tasks, users);

            if (newFitness > currentFitness) {
                currentFitness = newFitness;
                individual.setFitness(currentFitness);
            } else {
                individual.setGene(taskIndex, originalWorkerIndex);
                individual.setFitness(currentFitness);
            }
        }
    }
}
