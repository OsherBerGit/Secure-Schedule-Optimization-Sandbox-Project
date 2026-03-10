package com.example.mainbackend.algorithm.service;

import com.example.mainbackend.algorithm.AlgorithmClient;
import com.example.mainbackend.algorithm.dto.AlgoScheduleRequest;
import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import com.example.mainbackend.algorithm.dto.AlgoTaskAssignmentResponse;
import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.constants.TaskStatusConstants;
import com.example.mainbackend.entity.Settlement;
import com.example.mainbackend.entity.SettlementStatus;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.mapper.TaskMapper;
import com.example.mainbackend.mapper.UserMapper;
import com.example.mainbackend.repository.SettlementRepository;
import com.example.mainbackend.repository.SettlementStatusRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.repository.TaskStatusRepository;
import com.example.mainbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the full scheduling flow:
 *  1. Fetch the active scheduling configuration (weights, population size, etc.)
 *  2. Fetch only OPEN tasks — Zero-Trust: LOCKED/SCHEDULED/CLOSED tasks are never sent to the algorithm
 *  3. Build a minimal, anonymous request (no PII) for the algorithm service
 *  4. Apply results: Task → SCHEDULED (lifecycle), Settlement → ASSIGNED (execution)
 *
 * Rules enforced:
 *  - No Lombok — manual constructor injection
 *  - Zero-Trust — only IDs and capacity data leave this service to the algorithm
 *  - Mapper Pattern — entity-to-DTO conversion delegated to TaskMapper / UserMapper
 *  - N+1 prevention — JOIN FETCH queries used for tasks and users
 */
@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SettlementRepository settlementRepository;
    private final SchedulingConfigurationService configService;
    private final AlgorithmClient algorithmClient;
    private final TaskStatusRepository taskStatusRepository;
    private final SettlementStatusRepository settlementStatusRepository;

    // ── Manual Constructor (No Lombok) ────────────────────────────────────────

    public SchedulingService(UserRepository userRepository,
                             TaskRepository taskRepository,
                             SettlementRepository settlementRepository,
                             SchedulingConfigurationService configService,
                             AlgorithmClient algorithmClient,
                             TaskStatusRepository taskStatusRepository,
                             SettlementStatusRepository settlementStatusRepository) {
        this.userRepository              = userRepository;
        this.taskRepository              = taskRepository;
        this.settlementRepository        = settlementRepository;
        this.configService               = configService;
        this.algorithmClient             = algorithmClient;
        this.taskStatusRepository        = taskStatusRepository;
        this.settlementStatusRepository  = settlementStatusRepository;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    @Transactional
    public AlgoScheduleResponse runScheduling(String strategy) {
        // 1. Load the active scheduling configuration (weights, GA params)
        SchedulingConfigurationDto config = configService.getActiveConfiguration();

        // 2. Build an anonymous, minimal request — no PII leaves this boundary
        AlgoScheduleRequest request = buildRequest(strategy, config);

        // 3. Delegate to the algorithm engine
        AlgoScheduleResponse response = algorithmClient.requestSchedule(request);

        // 4. Persist results: Task → SCHEDULED, Settlement → ASSIGNED
        applyResults(response);

        return response;
    }

    // ── Build request ─────────────────────────────────────────────────────────

    private AlgoScheduleRequest buildRequest(String strategy, SchedulingConfigurationDto config) {
        List<com.example.mainbackend.algorithm.dto.AlgoUserRequest> users = buildUserRequests();
        List<com.example.mainbackend.algorithm.dto.AlgoTaskRequest> tasks = buildTaskRequests();

        log.info("Scheduling request: strategy={}, users={}, tasks={}",
                strategy, users.size(), tasks.size());

        return AlgoScheduleRequest.builder()
                .strategy(strategy)
                .config(config)
                .users(users)
                .tasks(tasks)
                .build();
    }

    /**
     * Loads all workers with their roles in a single JOIN FETCH query.
     * Vacations are embedded in each User entity (EAGER via @OneToMany) and filtered
     * to APPROVED inside UserMapper.toAlgoRequest — no PII is included.
     */
    private List<com.example.mainbackend.algorithm.dto.AlgoUserRequest> buildUserRequests() {
        return userRepository.findAllWithRoles().stream()
                .map(UserMapper::toAlgoRequest)
                .collect(Collectors.toList());
    }

    /**
     * Zero-Trust filter: only OPEN tasks are sent to the algorithm.
     * Two JOIN FETCH queries are used (roles + constraints) to avoid a Cartesian product
     * when fetching two collections simultaneously.
     *
     * The two result sets are merged in-memory by task ID so the mapper sees a fully
     * populated entity with both collections available.
     */
    private List<com.example.mainbackend.algorithm.dto.AlgoTaskRequest> buildTaskRequests() {
        // Fetch roles and constraints in two separate queries to prevent HHH90003004
        List<Task> tasksWithRoles       = taskRepository.findOpenTasksWithRoles(TaskStatusConstants.TASK_OPEN);
        List<Task> tasksWithConstraints = taskRepository.findOpenTasksWithConstraints(TaskStatusConstants.TASK_OPEN);

        // Index the constraints result by task ID for O(1) merge
        Map<Long, Task> constraintMap = tasksWithConstraints.stream()
                .collect(Collectors.toMap(Task::getId, t -> t, (a, b) -> a));

        // Merge: use the roles-loaded entity but copy the constraints collection across
        List<Task> mergedTasks = tasksWithRoles.stream().peek(task -> {
            Task withConstraints = constraintMap.get(task.getId());
            if (withConstraints != null) {
                // Replace the lazy-proxy collection with the already-fetched one
                task.setIncomingConstraints(withConstraints.getIncomingConstraints());
            }
        }).toList();

        log.info("Sending {} OPEN tasks to algorithm (LOCKED/SCHEDULED/CLOSED excluded)", mergedTasks.size());

        // Build the set of OPEN task IDs so the mapper can strip predecessor references
        // that point to tasks not in this scheduling run (already SCHEDULED/LOCKED/CLOSED).
        // Without this filter the algorithm rejects any task whose predecessor has already
        // been scheduled (it receives an ID it has never seen).
        Set<Long> openTaskIds = mergedTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        return mergedTasks.stream()
                .map(task -> TaskMapper.toAlgoRequest(task, openTaskIds))
                .collect(Collectors.toList());
    }

    // ── Apply results ─────────────────────────────────────────────────────────

    /**
     * Persists the algorithm's output:
     *  - Task lifecycle → SCHEDULED (algorithm has committed to this assignment)
     *  - Settlement execution → ASSIGNED (worker has an active assignment)
     *
     * Idempotent: an existing settlement for the same (task, worker) pair is skipped
     * to prevent duplicates on retry/re-run.
     */
    private void applyResults(AlgoScheduleResponse response) {
        if (response == null || response.getAssignments() == null) return;

        TaskStatus scheduledStatus = taskStatusRepository
                .findByName(TaskStatusConstants.TASK_SCHEDULED)
                .orElseThrow(() -> new IllegalStateException(
                        "TaskStatus '" + TaskStatusConstants.TASK_SCHEDULED + "' not seeded in task_statuses table"));

        SettlementStatus assignedStatus = settlementStatusRepository
                .findByName(TaskStatusConstants.SETTLEMENT_ASSIGNED)
                .orElseThrow(() -> new IllegalStateException(
                        "SettlementStatus '" + TaskStatusConstants.SETTLEMENT_ASSIGNED + "' not seeded in settlement_statuses table"));

        // Pre-load all affected task IDs to avoid repeated DB round-trips inside the loop
        Set<Long> taskIds = response.getAssignments().stream()
                .map(AlgoTaskAssignmentResponse::getTaskId)
                .collect(Collectors.toSet());

        Map<Long, Task> taskCache = taskRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        for (AlgoTaskAssignmentResponse assignment : response.getAssignments()) {
            if (assignment.getAssignedUserId() == null || assignment.getTaskId() == null) continue;

            Task task = taskCache.get(assignment.getTaskId());
            if (task == null) {
                log.warn("Algorithm returned unknown taskId={}, skipping", assignment.getTaskId());
                continue;
            }

            userRepository.findById(assignment.getAssignedUserId()).ifPresentOrElse(user -> {

                // ── Step 1: Task lifecycle → SCHEDULED ──────────────────────
                task.setStartTime(assignment.getScheduledStart());
                task.setStatus(scheduledStatus);
                taskRepository.save(task);

                // ── Step 2: Settlement → ASSIGNED (idempotent guard) ─────────
                boolean alreadySettled = settlementRepository.findByTaskId(task.getId()).stream()
                        .anyMatch(s -> s.getWorker().getId().equals(user.getId()));

                if (!alreadySettled) {
                    settlementRepository.save(Settlement.builder()
                            .task(task)
                            .worker(user)
                            .status(assignedStatus)
                            .settlementDate(LocalDateTime.now())
                            .build());

                    log.debug("Task [{}] → SCHEDULED; Settlement → ASSIGNED for worker [{}]",
                            task.getId(), user.getId());
                } else {
                    log.debug("Skipping duplicate settlement for task [{}] / worker [{}]",
                            task.getId(), user.getId());
                }

            }, () -> log.warn("Algorithm returned unknown userId={}, skipping", assignment.getAssignedUserId()));
        }

        log.info("Applied {} assignments: tasks SCHEDULED, settlements ASSIGNED",
                response.getAssignedTasks());
    }
}
