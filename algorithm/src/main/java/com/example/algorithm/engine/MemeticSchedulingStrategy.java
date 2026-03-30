package com.example.algorithm.engine;

import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.engine.core.EvolutionaryOperators;
import com.example.algorithm.engine.core.FitnessEvaluator;
import com.example.algorithm.engine.core.Individual;
import com.example.algorithm.engine.core.LocalSearch;
import com.example.algorithm.model.AlgoSchedulingConfiguration;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Memetic Algorithm (Hybrid Genetic Algorithm + Local Search).
 *
 * <p>Combines a Genetic Algorithm (population-based global search) with a
 * Hill-Climbing Local Search (individual refinement) to find high-quality
 * task-to-worker assignments.</p>
 *
 * <h3>Algorithm overview</h3>
 * <ol>
 *   <li><b>Initialisation</b> — generate a random population of chromosomes,
 *       where each gene {@code i} holds the worker-list index assigned to task {@code i}.</li>
 *   <li><b>Evolution loop</b> (repeated for {@code maxGenerations} generations):
 *     <ul>
 *       <li>Evaluate fitness of every unevaluated individual.</li>
 *       <li>Sort population best-first.</li>
 *       <li>Preserve the single best individual (elitism).</li>
 *       <li>Fill the rest of the new population via tournament selection →
 *           uniform crossover → mutation → local search.</li>
 *     </ul>
 *   </li>
 *   <li><b>Decode</b> — translate the best chromosome into {@link TaskAssignment} objects
 *       using the same constraint pipeline inherited from {@link BaseSchedulingStrategy}.</li>
 * </ol>
 *
 * <p>Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 */
public class MemeticSchedulingStrategy extends BaseSchedulingStrategy {

    private static final int TOURNAMENT_SIZE = 3;
    private static final int LOCAL_SEARCH_ITERS = 5;
    private static final double MUTATION_RATE = 0.05;

    private final AlgoSchedulingConfiguration config;
    private final FitnessEvaluator fitnessEvaluator;
    private final EvolutionaryOperators operators;
    private final LocalSearch localSearch;
    private final Random random = new Random();

    private final List<Double> fitnessHistory = new ArrayList<>();

    public MemeticSchedulingStrategy(AlgoSchedulingConfiguration config) {
        this.config = config;
        this.fitnessEvaluator = new FitnessEvaluator(this.hardConstraints, this.softScorers, config);
        this.operators = new EvolutionaryOperators();
        this.localSearch = new LocalSearch(this.fitnessEvaluator);
    }

    @Override
    public String getName() {
        return "MEMETIC";
    }

    public List<Double> getFitnessHistory() {
        return java.util.Collections.unmodifiableList(fitnessHistory);
    }

    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<AlgoTask> tasks = getSortedUnassignedTasks(data.tasks());
        List<AlgoUser> users = data.users();

        if (tasks.isEmpty() || users.isEmpty())
            return new ArrayList<>();

        int popSize = config.getPopulationSize() != null ? config.getPopulationSize() : 50;
        int maxGenerations = config.getMaxGenerations() != null ? config.getMaxGenerations() : 100;

        List<Individual> population = initializePopulation(popSize, tasks, users, data);
        fitnessHistory.clear();

        for (int generation = 0; generation < maxGenerations; generation++) {
            evaluatePopulation(population, tasks, users);
            population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());
            fitnessHistory.add(population.get(0).getFitness());

            List<Individual> nextGeneration = new ArrayList<>();
            nextGeneration.add(new Individual(population.get(0))); // Elitism

            while (nextGeneration.size() < popSize) {
                Individual parent1 = tournamentSelection(population);
                Individual parent2 = tournamentSelection(population);
                Individual child = operators.crossover(parent1, parent2);
                operators.mutate(child, users.size(), MUTATION_RATE);
                localSearch.optimize(child, tasks, users, LOCAL_SEARCH_ITERS);
                nextGeneration.add(child);
            }
            population = nextGeneration;
        }

        evaluatePopulation(population, tasks, users);
        population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());
        Individual bestSolution = population.get(0);

        return decodeChromosome(bestSolution, tasks, users);
    }

    /**
     * Creates an initial population, seeding it with one heuristically-generated
     * individual (using a greedy method) and filling the rest randomly. This
     * "Heuristic Start" is crucial for problems with tight constraints, as it
     * guarantees at least one valid individual exists in the initial population.
     */
    private List<Individual> initializePopulation(int popSize, List<AlgoTask> tasks, List<AlgoUser> users, ScheduleData data) {
        List<Individual> population = new ArrayList<>();

        // 1. Generate the Greedy seed directly using the existing strategy
        GreedySchedulingStrategy greedyStrategy = new GreedySchedulingStrategy();
        List<TaskAssignment> greedyAssignments = greedyStrategy.schedule(data);

        Individual greedyIndividual = new Individual(tasks.size());

        // Map user DB IDs to their list index for correct gene encoding
        Map<Long, Integer> userIndexMap = new HashMap<>();
        for (int i = 0; i < users.size(); i++)
            userIndexMap.put(users.get(i).getId(), i);

        Map<Long, AlgoUser> greedyResultsMap = new HashMap<>();
        for (TaskAssignment ta : greedyAssignments)
            greedyResultsMap.put(ta.getTask().getId(), ta.getAssignedEmployee());

        // Translate Greedy assignments into the chromosome
        for (int i = 0; i < tasks.size(); i++) {
            AlgoTask currentTask = tasks.get(i);

            AlgoUser assignedWorker = greedyResultsMap.get(currentTask.getId());

            if (assignedWorker != null && userIndexMap.containsKey(assignedWorker.getId()))
                greedyIndividual.setGene(i, userIndexMap.get(assignedWorker.getId()));
            else
                greedyIndividual.setGene(i, -1);
        }
        population.add(greedyIndividual);

        // 2. Fill the rest randomly
        while (population.size() < popSize) {
            Individual randomIndividual = new Individual(tasks.size());
            for (int i = 0; i < tasks.size(); i++)
                randomIndividual.setGene(i, random.nextInt(users.size()));
            population.add(randomIndividual);
        }

        return population;
    }

    /**
     * Evaluates fitness for every individual in the population whose score is stale.
     * Skips individuals whose fitness is already up-to-date to avoid redundant work.
     */
    private void evaluatePopulation(List<Individual> population, List<AlgoTask> tasks, List<AlgoUser> users) {
        for (Individual ind : population)
            if (!ind.isFitnessCalculated())
                ind.setFitness(fitnessEvaluator.evaluate(ind, tasks, users));
    }

    /**
     * Selects one parent using tournament selection. Randomly picks a few individuals
     * and returns the one with the best fitness.
     */
    private Individual tournamentSelection(List<Individual> population) {
        Individual best = population.get(random.nextInt(population.size()));
        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            Individual competitor = population.get(random.nextInt(population.size()));
            if (competitor.getFitness() > best.getFitness())
                best = competitor;
        }
        return best;
    }

    /**
     * Translates the best chromosome into a list of {@link TaskAssignment} objects.
     * This is the final, strict decoding step that produces the output schedule.
     * It uses the exact same constraint validation and time calculation as the
     * fitness evaluation to ensure consistency.
     */
    private List<TaskAssignment> decodeChromosome(Individual best, List<AlgoTask> tasks, List<AlgoUser> users) {
        List<TaskAssignment> assignments = new ArrayList<>();
        Map<Long, Integer> assignedCount = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();
        Map<Long, TaskAssignment> assignmentsMap = new HashMap<>();
        Map<Long, LocalDateTime> workerNextFree = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        for (AlgoUser user : users) {
            assignedCount.put(user.getId(), 0);
            workerNextFree.put(user.getId(), now);
        }

        int[] chromosome = best.getChromosome();

        for (int i = 0; i < chromosome.length; i++) {
            AlgoTask task = tasks.get(i);
            int workerIndex = chromosome[i];

            // A gene value of -1 or an index outside the user list bounds means the task is unassigned.
            if (workerIndex < 0 || workerIndex >= users.size()) {
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason("Memetic: Gene was unassigned or out of range.")
                        .build());
                continue;
            }

            AlgoUser assignedUser = users.get(workerIndex);

            // Find the next valid start time using the same logic as the fitness evaluator.
            Optional<LocalDateTime> possibleStart = findNextAvailableStartTime(task, assignedUser, assignmentsMap, workerNextFree.get(assignedUser.getId()));

            if (possibleStart.isPresent()) {
                LocalDateTime start = possibleStart.get();
                LocalDateTime end = calcEndTime(start, task);

                // Run the final, definitive constraint check.
                ConstraintResult result = validateHardConstraints(
                        task, assignedUser, start, end,
                        completionTimes, assignedCount, assignments);

                if (result.isValid()) {
                    // If valid, update state and create the assignment.
                    workerNextFree.put(assignedUser.getId(), end);
                    completionTimes.put(task.getId(), end);
                    assignedCount.merge(assignedUser.getId(), 1, Integer::sum);

                    TaskAssignment newAssignment = TaskAssignment.builder()
                            .task(task)
                            .assignedEmployee(assignedUser)
                            .scheduledStart(start)
                            .scheduledEnd(end)
                            .reason("Memetic: best chromosome assignment")
                            .build();

                    assignments.add(newAssignment);
                    assignmentsMap.put(task.getId(), newAssignment);
                } else
                    // If the gene is invalid even after fitness evaluation and local search, mark as unassigned.
                    assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason("Memetic: Constraint violation during decode: " + result.getReason())
                        .build());
            } else
                 // This case should be rare if fitness evaluation is correct, but is a safeguard.
                 assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason("Memetic: No available shift found during decode.")
                        .build());
        }

        return assignments;
    }
}
