package com.example.algorithm.engine;

import com.example.algorithm.constraint.AvailabilityConstraint;
import com.example.algorithm.constraint.ConstraintChecker;
import com.example.algorithm.constraint.ConstraintResult;
import com.example.algorithm.constraint.DeadlineConstraint;
import com.example.algorithm.constraint.PrecedenceConstraint;
import com.example.algorithm.db.ScheduleData;
import com.example.algorithm.engine.core.EvolutionaryOperators;
import com.example.algorithm.engine.core.FitnessEvaluator;
import com.example.algorithm.engine.core.Individual;
import com.example.algorithm.engine.core.MemeticLocalSearch;
import com.example.algorithm.model.AlgoSchedulingConfiguration;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Memetic Algorithm Scheduling Strategy.
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

    private static final int    TOURNAMENT_SIZE  = 3;
    private static final int    LOCAL_SEARCH_ITERS = 5;
    private static final double MUTATION_RATE    = 0.05;

    private final AlgoSchedulingConfiguration config;
    private final FitnessEvaluator            fitnessEvaluator;
    private final EvolutionaryOperators       operators;
    private final MemeticLocalSearch          localSearch;
    private final Random                      random = new Random();

    /** Best fitness score recorded at each generation. Populated after {@link #schedule} runs. */
    private final List<Double> fitnessHistory = new ArrayList<>();

    /**
     * @param config    GA weights and parameters (population size, generations, fitness weights)
     * @param evaluator the fitness evaluator wired with the same constraint pipeline
     */
    public MemeticSchedulingStrategy(AlgoSchedulingConfiguration config,
                                      FitnessEvaluator evaluator) {
        this.config           = config;
        this.fitnessEvaluator = evaluator;
        this.operators        = new EvolutionaryOperators();
        this.localSearch      = new MemeticLocalSearch(evaluator);
    }

    /**
     * Convenience factory — builds a {@code MemeticSchedulingStrategy} pre-wired with the
     * standard constraint pipeline ({@code PrecedenceConstraint}, {@code DeadlineConstraint},
     * {@code AvailabilityConstraint}) and a {@link FitnessEvaluator} that uses the same
     * constraints and weights as the supplied configuration.
     *
     * <p>Use this factory in {@code AlgoService} so callers do not need to know about the
     * internal {@link ConstraintChecker} list:</p>
     * <pre>{@code
     *   MemeticSchedulingStrategy.withDefaults(config)
     * }</pre>
     *
     * @param config GA weights and algorithm parameters
     * @return a fully-wired {@code MemeticSchedulingStrategy} instance
     */
    public static MemeticSchedulingStrategy withDefaults(AlgoSchedulingConfiguration config) {
        List<ConstraintChecker> pipeline = List.of(
                new PrecedenceConstraint(),
                new DeadlineConstraint(),
                new AvailabilityConstraint()
        );
        FitnessEvaluator evaluator = new FitnessEvaluator(pipeline, config);
        return new MemeticSchedulingStrategy(config, evaluator);
    }

    @Override
    public String getName() {
        return "MEMETIC";
    }

    /** Returns the best fitness score recorded per generation. Only valid after {@link #schedule} runs. */
    public List<Double> getFitnessHistory() {
        return java.util.Collections.unmodifiableList(fitnessHistory);
    }

    /**
     * Runs the Memetic Algorithm and returns the decoded list of task assignments.
     *
     * @param data snapshot containing the tasks and users to schedule
     * @return list of {@link TaskAssignment} objects (one per successfully decoded task)
     */
    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<AlgoTask> tasks = data.tasks();
        List<AlgoUser> users = data.users();

        // Graceful early exit: nothing to schedule.
        if (tasks.isEmpty() || users.isEmpty()) {
            return new ArrayList<>();
        }

        int popSize        = config.getPopulationSize()  != null ? config.getPopulationSize()  : 50;
        int maxGenerations = config.getMaxGenerations()  != null ? config.getMaxGenerations()  : 100;

        // ── 1. Initialisation ────────────────────────────────────────────────
        List<Individual> population = initializePopulation(popSize, tasks.size(), users.size());
        fitnessHistory.clear();

        // ── 2. Evolution loop ────────────────────────────────────────────────
        for (int generation = 0; generation < maxGenerations; generation++) {

            // Evaluate fitness for every individual whose score is stale.
            evaluatePopulation(population, tasks, users);

            // Sort population: highest fitness first.
            population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());

            // Record the best fitness of this generation for convergence tracking.
            fitnessHistory.add(population.get(0).getFitness());

            List<Individual> nextGeneration = new ArrayList<>();

            // Elitism: always carry the current best individual forward unchanged.
            nextGeneration.add(new Individual(population.get(0)));

            // Fill the remainder of the new generation.
            while (nextGeneration.size() < popSize) {
                // Tournament selection for both parents.
                Individual parent1 = tournamentSelection(population);
                Individual parent2 = tournamentSelection(population);

                // Uniform crossover produces one child.
                Individual child = operators.crossover(parent1, parent2);

                // Random mutation with a small probability per gene.
                operators.mutate(child, users.size(), MUTATION_RATE);

                // Memetic refinement: hill-climbing local search on the child.
                localSearch.optimize(child, tasks, users, LOCAL_SEARCH_ITERS);

                nextGeneration.add(child);
            }

            population = nextGeneration;
        }

        // ── 3. Final evaluation and selection of the best solution ───────────
        evaluatePopulation(population, tasks, users);
        population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());
        Individual bestSolution = population.get(0);

        // ── 4. Decode the winning chromosome into TaskAssignment objects ──────
        return decodeChromosome(bestSolution, tasks, users);
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * Creates a population of {@code popSize} randomly initialised individuals.
     * Each gene is assigned a uniformly random worker index in {@code [0, numUsers)}.
     */
    private List<Individual> initializePopulation(int popSize, int numTasks, int numUsers) {
        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            Individual ind = new Individual(numTasks);
            for (int t = 0; t < numTasks; t++) {
                ind.setGene(t, random.nextInt(numUsers));
            }
            population.add(ind);
        }
        return population;
    }

    // -------------------------------------------------------------------------
    // Fitness evaluation
    // -------------------------------------------------------------------------

    /**
     * Evaluates fitness for every individual in the population whose score is stale.
     * Skips individuals whose fitness is already up-to-date to avoid redundant work.
     */
    private void evaluatePopulation(List<Individual> population,
                                    List<AlgoTask> tasks,
                                    List<AlgoUser> users) {
        for (Individual ind : population) {
            if (!ind.isFitnessCalculated()) {
                ind.setFitness(fitnessEvaluator.evaluate(ind, tasks, users));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tournament selection
    // -------------------------------------------------------------------------

    /**
     * Selects one parent using tournament selection.
     *
     * <p>Randomly picks {@link #TOURNAMENT_SIZE} individuals from the population and
     * returns the one with the highest fitness.  Tournament selection provides
     * selection pressure while maintaining diversity better than roulette-wheel
     * selection in the presence of large fitness disparities.</p>
     */
    private Individual tournamentSelection(List<Individual> population) {
        Individual best = population.get(random.nextInt(population.size()));
        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            Individual competitor = population.get(random.nextInt(population.size()));
            if (competitor.getFitness() > best.getFitness()) {
                best = competitor;
            }
        }
        return best;
    }

    // -------------------------------------------------------------------------
    // Chromosome decoding
    // -------------------------------------------------------------------------

    /**
     * Translates the best chromosome into a list of {@link TaskAssignment} objects.
     *
     * <p>Mirrors the greedy decode loop: maintains a per-worker availability timeline
     * and a completion-time map so that precedence and deadline constraints are
     * evaluated correctly.  Tasks that fail the constraint pipeline are added as
     * unassigned entries (same convention as {@link GreedySchedulingStrategy}) so
     * the caller always receives one entry per task.</p>
     *
     * @param best  the winning individual
     * @param tasks ordered task list (chromosome index → task)
     * @param users ordered user list (chromosome value → user)
     * @return list of {@link TaskAssignment}, one per task in {@code tasks}
     */
    private List<TaskAssignment> decodeChromosome(Individual best,
                                                   List<AlgoTask> tasks,
                                                   List<AlgoUser> users) {
        List<TaskAssignment>     assignments     = new ArrayList<>();
        Map<Long, Integer>       assignedCount   = new HashMap<>();
        Map<Long, LocalDateTime> completionTimes = new HashMap<>();
        Map<Long, LocalDateTime> workerNextFree  = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        for (AlgoUser user : users) {
            assignedCount.put(user.getId(), 0);
            workerNextFree.put(user.getId(), now);
        }

        int[] chromosome = best.getChromosome();

        for (int i = 0; i < chromosome.length; i++) {
            AlgoTask task        = tasks.get(i);
            int      workerIndex = chromosome[i];

            // Gene out of range means the GA chose not to assign this task.
            if (workerIndex < 0 || workerIndex >= users.size()) {
                LocalDateTime start = calcStartTime(task, completionTimes);
                LocalDateTime end   = calcEndTime(start, task);
                completionTimes.put(task.getId(), end);
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .scheduledStart(start)
                        .scheduledEnd(end)
                        .reason("Memetic: gene out of range — task left unassigned")
                        .build());
                continue;
            }

            AlgoUser assignedUser = users.get(workerIndex);

            // Proposed start: the later of the worker's next free slot and any predecessor end.
            // getPredecessorTaskIds() is always non-null (returns Collections.emptyList() when absent).
            LocalDateTime proposedStart = workerNextFree.get(assignedUser.getId());
            for (Long predId : task.getPredecessorTaskIds()) {
                LocalDateTime predEnd = completionTimes.get(predId);
                if (predEnd != null && predEnd.isAfter(proposedStart)) {
                    proposedStart = predEnd;
                }
            }
            LocalDateTime proposedEnd = calcEndTime(proposedStart, task);

            // Run the full inherited constraint pipeline as a final validation gate.
            ConstraintResult result = runConstraints(
                    task, assignedUser, proposedStart, proposedEnd,
                    completionTimes, assignedCount);

            if (result.isValid()) {
                workerNextFree.put(assignedUser.getId(), proposedEnd);
                completionTimes.put(task.getId(), proposedEnd);
                assignedCount.merge(assignedUser.getId(), 1, Integer::sum);

                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(assignedUser)
                        .scheduledStart(proposedStart)
                        .scheduledEnd(proposedEnd)
                        .reason("Memetic: best chromosome assignment")
                        .build());
            } else {
                // The GA produced a chromosome that still violates a hard constraint after
                // penalties — record as unassigned so the caller knows about the gap.
                completionTimes.put(task.getId(), proposedEnd);
                assignments.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .scheduledStart(proposedStart)
                        .scheduledEnd(proposedEnd)
                        .reason("Memetic: constraint violation during decode — "
                                + result.getReason())
                        .build());
            }
        }

        return assignments;
    }
}