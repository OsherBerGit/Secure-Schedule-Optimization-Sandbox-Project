package com.example.algorithm.engine;

import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Constraint Programming (CP) Scheduling Strategy using Choco Solver.
 *
 * <p>Uses the Choco Solver library (a Constraint Programming solver) to find a
 * mathematically valid schedule that satisfies all constraints simultaneously.</p>
 *
 * <p>Key Characteristics:</p>
 * <ul>
 *   <li>Models the scheduling problem as a CSP (Constraint Satisfaction Problem).</li>
 *   <li>Time is discretized into minutes from the start of the week (0 to 10080).</li>
 *   <li>Variables:
 *     <ul>
 *       <li>Start Time: When each task begins (0..10080).</li>
 *       <li>Assignee: Which worker performs the task (domain: eligible workers).</li>
 *     </ul>
 *   </li>
 *   <li>Constraints:
 *     <ul>
 *       <li>Precedence: Task B cannot start until Task A ends.</li>
 *       <li>Resources: Workers have limited availability (shifts) and capacity (1 task at a time).</li>
 *       <li>No Overlap: A worker cannot perform two tasks simultaneously (Cumulative constraint).</li>
 *       <li>Deadlines: Tasks must finish by their deadline (or end of week).</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>If a valid solution exists, it returns the optimal assignment. If no solution
 * is found within the time limit (5s), it returns a failure explanation.</p>
 *
 * <h3>Complexity Analysis</h3>
 * <ul>
 *   <li><b>Time Complexity:</b> Exponential (NP-Hard), but optimized via pruning.</li>
 *   <li><b>Variables:</b>
 *     <ul>
 *       <li>N = Number of Tasks (Variables)</li>
 *       <li>D = Domain Size (Number of eligible workers + time slots)</li>
 *       <li>C = Number of Constraints</li>
 *     </ul>
 *   </li>
 *   <li><b>Explanation:</b>
 *     <ul>
 *       <li>Solving a Constraint Satisfaction Problem (CSP) is generally NP-Hard. In the worst case, backtracking search explores O(D^N) states.</li>
 *       <li>However, constraint propagation (Pruning) significantly reduces the search space by eliminating impossible values early.</li>
 *       <li>The <b>Arc Consistency</b> algorithms used by Choco (e.g., AC-3) run in polynomial time to reduce domains before search.</li>
 *       <li>We enforce a hard time limit (e.g., 5 seconds) to prevent infinite searching, effectively capping the runtime constant.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class ConstraintProgrammingStrategy extends BaseSchedulingStrategy {

    private static final int MINUTES_IN_DAY = 24 * 60;
    private static final int MINUTES_IN_WEEK = 7 * MINUTES_IN_DAY;

    @Override
    public String getName() { return "CONSTRAINT_PROGRAMMING"; }

    /**
     * Executes the CP solver to find a valid schedule.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Setup Time Anchor: Use Monday 00:00 as minute 0.</li>
     *   <li>Initialize Choco Model.</li>
     *   <li>Create Variables: Task start times, durations, ends, and assignees.</li>
     *   <li>Post Task Constraints: Precedence and valid domains.</li>
     *   <li>Post Resource Constraints: Worker availability windows and non-overlapping tasks.</li>
     *   <li>Solve: Search for a solution with a timeout.</li>
     * </ol>
     */
    @Override
    public List<TaskAssignment> schedule(ScheduleData data) {
        List<AlgoTask> tasks = data.tasks();
        List<AlgoUser> users = data.users();

        if (tasks == null || tasks.isEmpty() || users == null || users.isEmpty())
            return Collections.emptyList();

        // 1. Time Setup: Anchor is Monday 00:00 of the current week
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate anchorDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime anchor = anchorDate.atStartOfDay();

        // 2. Model
        Model model = new Model("Constraint Scheduling");

        int nTasks = tasks.size();
        int nUsers = users.size();
        int dummyUserIdx = nUsers; // Virtual worker index for "Unscheduled"

        IntVar[] taskStarts = new IntVar[nTasks];
        IntVar[] taskAssignees = new IntVar[nTasks];
        IntVar[] taskDurations = new IntVar[nTasks];
        IntVar[] taskEnds = new IntVar[nTasks];

        Map<Long, Integer> taskIndexMap = new HashMap<>();
        for (int i = 0; i < nTasks; i++) taskIndexMap.put(tasks.get(i).getId(), i);

        // 3. Variables & Task Constraints
        for (int i = 0; i < nTasks; i++) {
            AlgoTask task = tasks.get(i);
            int durationMins = task.getDurationHours() * 60;
            
            // Calculate deadline in minutes from anchor
            long deadlineMinsLong = ChronoUnit.MINUTES.between(anchor, task.getDeadline());
            int deadlineMins = (int) Math.min(deadlineMinsLong, MINUTES_IN_WEEK);

            // Relaxed Time Constraint: Allow scheduling from start of week (0)
            int earliest = 0; 
            int latest = deadlineMins - durationMins;

            if (latest < earliest)
                 // Even with relaxation, if deadline is before Monday 00:00 or duration is too long
                 // we must force assignment to dummy, and allow any start time in valid range to avoid domain fail
                 taskStarts[i] = model.intVar("start_" + task.getId(), 0, MINUTES_IN_WEEK);
            else
                 taskStarts[i] = model.intVar("start_" + task.getId(), earliest, latest);

            taskDurations[i] = model.intVar(durationMins);
            taskEnds[i] = taskStarts[i].add(taskDurations[i]).intVar();

            // Assignee domain: eligible users + Dummy
            List<Integer> validIndices = new ArrayList<>();
            for (int u = 0; u < nUsers; u++)
                if (hasRequiredJob(users.get(u), task))
                    validIndices.add(u);
            
            // ALWAYS add dummy worker to allow skipping
            validIndices.add(dummyUserIdx);
            
            int[] domain = validIndices.stream().mapToInt(Integer::intValue).toArray();
            taskAssignees[i] = model.intVar("assignee_" + task.getId(), domain);

            // If strict time impossible, force dummy
            if (latest < earliest)
                model.arithm(taskAssignees[i], "=", dummyUserIdx).post();
        }

        // 4. Precedence Constraints
        for (int i = 0; i < nTasks; i++) {
            AlgoTask task = tasks.get(i);
            if (task.getPredecessorTaskIds() != null)
                for (Long predId : task.getPredecessorTaskIds())
                    if (taskIndexMap.containsKey(predId)) {
                        int predIdx = taskIndexMap.get(predId);
                        // Start of this task >= End of predecessor
                        model.arithm(taskStarts[i], ">=", taskEnds[predIdx]).post();
                    }
        }

        // 5. User Constraints (Availability & Capacity)
        // Loop only REAL users (0 to nUsers-1). Dummy user has no constraints.
        for (int u = 0; u < nUsers; u++) {
            AlgoUser user = users.get(u);
            
            List<Task> cumulativeTasks = new ArrayList<>();
            List<IntVar> cumulativeHeights = new ArrayList<>();

            // A. Blocked Time as fixed tasks (height 1)
            List<int[]> blockedIntervals = calculateBlockedIntervals(user, anchor);
            for (int[] interval : blockedIntervals) {
                int start = interval[0];
                int end = interval[1];
                if (end > start) {
                    IntVar bStart = model.intVar(start);
                    IntVar bDur = model.intVar(end - start);
                    IntVar bEnd = model.intVar(end);
                    Task bTask = new Task(bStart, bDur, bEnd);
                    cumulativeTasks.add(bTask);
                    cumulativeHeights.add(model.intVar(1)); 
                }
            }

            // B. Potential Tasks
            List<IntVar> userAssignedVars = new ArrayList<>(); // Track for maxTasks constraint
            
            for (int i = 0; i < nTasks; i++) {
                // Determine if task i is assigned to user u
                BoolVar isAssigned = model.boolVar("assigned_" + i + "_" + u);
                
                // Reify assignment: isAssigned <=> (taskAssignees[i] == u)
                model.ifOnlyIf(
                    model.arithm(taskAssignees[i], "=", u),
                    model.arithm(isAssigned, "=", 1)
                );

                cumulativeTasks.add(new Task(taskStarts[i], taskDurations[i], taskEnds[i]));
                cumulativeHeights.add(isAssigned); // Height is 1 if assigned, 0 otherwise

                userAssignedVars.add(isAssigned);
            }

            // C. Post Cumulative constraint
            if (!cumulativeTasks.isEmpty()) {
                Task[] tArray = cumulativeTasks.toArray(new Task[0]);
                IntVar[] hArray = cumulativeHeights.toArray(new IntVar[0]);
                // Capacity is 1 (single worker)
                model.cumulative(tArray, hArray, model.intVar(1)).post();
            }

            // D. Max Tasks Limit
            if (user.getMaxTasks() != null && !userAssignedVars.isEmpty())
                 model.sum(userAssignedVars.toArray(new IntVar[0]), "<=", user.getMaxTasks()).post();
        }

        // 6. Objective: Maximize number of scheduled tasks
        BoolVar[] isScheduled = new BoolVar[nTasks];
        for (int i = 0; i < nTasks; i++) {
             isScheduled[i] = model.boolVar("isScheduled_" + i);
             // Scheduled if NOT assigned to dummy
             model.arithm(taskAssignees[i], "!=", dummyUserIdx).reifyWith(isScheduled[i]);
        }
        IntVar totalScheduled = model.intVar("totalScheduled", 0, nTasks);
        model.sum(isScheduled, "=", totalScheduled).post();
        
        model.setObjective(Model.MAXIMIZE, totalScheduled);

        // 7. Solve
        Solver solver = model.getSolver();
        solver.limitTime("5s"); 

        Solution bestSolution = new Solution(model);
        while(solver.solve())
             bestSolution.record();

        List<TaskAssignment> solution = new ArrayList<>();
        
        if (bestSolution.exists())
            for (int i = 0; i < nTasks; i++) {
                int uIdx = bestSolution.getIntVal(taskAssignees[i]);
                AlgoTask task = tasks.get(i);
                
                if (uIdx == dummyUserIdx)
                    solution.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason("No eligible worker or time slot found (CP)")
                        .build());
                else {
                    int startMins = bestSolution.getIntVal(taskStarts[i]);
                    AlgoUser user = users.get(uIdx);
                    LocalDateTime start = anchor.plusMinutes(startMins);
                    LocalDateTime end = start.plusMinutes(task.getDurationHours() * 60);
                    
                    solution.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(user)
                        .scheduledStart(start)
                        .scheduledEnd(end)
                        .reason("Optimal CP Solution")
                        .build());
                }
            }
        else
            // Should be rare given flexible dummy assignment
            for (AlgoTask task : tasks)
                solution.add(TaskAssignment.builder()
                        .task(task)
                        .assignedEmployee(null)
                        .reason("No valid solution found (Solver failed)")
                        .build());

        return solution;
    }

    private List<int[]> calculateBlockedIntervals(AlgoUser user, LocalDateTime anchor) {
        // 0..10080
        boolean[] isAvailable = new boolean[MINUTES_IN_WEEK];

        // 1. Mark availability from shift patterns
        if (user.getAvailabilities() != null)
            for (AlgoWorkerAvailability avail : user.getAvailabilities()) {
                int dayIndex = avail.dayOfWeek().getValue() - 1; // 0=Mon
                int startDayMins = dayIndex * MINUTES_IN_DAY;

                int startMins = startDayMins + avail.startTime().toSecondOfDay() / 60;
                int endMins = startDayMins + avail.endTime().toSecondOfDay() / 60;

                for (int m = startMins; m < endMins && m < MINUTES_IN_WEEK; m++)
                    isAvailable[m] = true;
            }

        // 2. Clear out Vacations
        if (user.getVacations() != null) {
            LocalDate anchorDate = anchor.toLocalDate();
            LocalDate weekEnd = anchorDate.plusDays(7);

            for (AlgoVacation vac : user.getVacations()) {
                LocalDate vStart = vac.getStartDate();
                LocalDate vEnd = vac.getEndDate();

                if (!vEnd.isBefore(anchorDate) && !vStart.isAfter(weekEnd)) {
                    LocalDate effStart = vStart.isBefore(anchorDate) ? anchorDate : vStart;
                    LocalDate effEnd = vEnd.isAfter(weekEnd) ? weekEnd : vEnd;

                    long startMinsLong = Duration.between(anchor, effStart.atStartOfDay()).toMinutes();
                    long endMinsLong = Duration.between(anchor, effEnd.plusDays(1).atStartOfDay()).toMinutes();

                    int s = (int) Math.max(0, startMinsLong);
                    int e = (int) Math.min(MINUTES_IN_WEEK, endMinsLong);

                    for (int m = s; m < e; m++)
                        isAvailable[m] = false;
                }
            }
        }

        // 3. Convert unavailable periods (isAvailable == false) to intervals
        List<int[]> blocked = new ArrayList<>();
        int start = -1;
        for (int m = 0; m < MINUTES_IN_WEEK; m++) {
            if (!isAvailable[m])
                if (start == -1) start = m;
            else
                if (start != -1) {
                    blocked.add(new int[]{start, m});
                    start = -1;
                }
        }
        if (start != -1)
            blocked.add(new int[]{start, MINUTES_IN_WEEK});
        return blocked;
    }

    private boolean hasRequiredJob(AlgoUser user, AlgoTask task) {
        Set<String> requiredJobs = task.getRequiredJobs();

        if (requiredJobs == null || requiredJobs.isEmpty()) return true;

        Set<String> userJobs = user.getJobs();
        if (userJobs == null || userJobs.isEmpty()) return false;

        return userJobs.containsAll(requiredJobs);
    }
}
