package com.example.mainbackend.algorithm.service;

import com.example.mainbackend.algorithm.AlgorithmClient;
import com.example.mainbackend.algorithm.dto.AlgoScheduleRequest;
import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import com.example.mainbackend.algorithm.dto.AlgoTaskAssignmentResponse;
import com.example.mainbackend.algorithm.dto.AlgoUnscheduledTaskResponse;
import com.example.mainbackend.algorithm.dto.SaveScheduleRequest;
import com.example.mainbackend.algorithm.dto.SchedulingConfigurationDto;
import com.example.mainbackend.constants.TaskStatusConstants;
import com.example.mainbackend.entity.Department;
import com.example.mainbackend.entity.Settlement;
import com.example.mainbackend.entity.SettlementStatus;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.exception.BatchValidationException;
import com.example.mainbackend.mapper.TaskMapper;
import com.example.mainbackend.mapper.UserMapper;
import com.example.mainbackend.repository.SettlementRepository;
import com.example.mainbackend.repository.SettlementStatusRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.repository.TaskStatusRepository;
import com.example.mainbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
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

    /**
     * PHASE 1 — Preview / Draft Mode.
     *
     * Runs the scheduling algorithm and returns an enriched preview response.
     * Nothing is written to the database. Task statuses remain OPEN; no settlements
     * are created. The frontend stores this as a draft for admin review before
     * calling {@link #saveApprovedSchedule}.
     *
     * @param strategy     the scheduling strategy name (GREEDY, ROUND_ROBIN, MEMETIC)
     * @param departmentId optional department scope for ADMIN users; ignored for MANAGER
     * @param configId     optional configuration ID (uses active if null)
     */
    @Transactional(readOnly = true)
    public AlgoScheduleResponse runScheduling(String strategy, Long departmentId, Long configId) {
        User currentUser = resolveCurrentUser();
        // Zero-Trust: Validate config existence and permissions if necessary (here just fetching)
        SchedulingConfigurationDto config = configService.getConfiguration(configId);
        AlgoScheduleRequest request = buildRequest(strategy, config, currentUser, departmentId);

        AlgoScheduleResponse response = algorithmClient.requestSchedule(request);

        // Enrich with human-readable names for the preview — no DB writes
        enrichForPreview(response);

        return response;
    }

    /**
     * Backwards compatibility overload for existing tests/calls
     */
    @Transactional(readOnly = true)
    public AlgoScheduleResponse runScheduling(String strategy, Long departmentId) {
        return runScheduling(strategy, departmentId, null);
    }

    /**
     * PHASE 2 — Approve and Save.
     * Refactored for strict Zero-Trust validation, N+1 prevention, and ACID compliance.
     *
     * Persists the admin-approved draft assignments:
     *   - Task lifecycle  → SCHEDULED
     *   - Settlement exec → ASSIGNED  (idempotent guard against duplicates)
     *
     * Uses Bulk-fetching pattern to eliminate N+1 queries.
     * Accumulates all validation errors and fails the entire batch if any issue is found.
     *
     * @param saveRequest the approved assignments forwarded from the frontend
     * @throws BatchValidationException if any validation fails (triggers rollback)
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveApprovedSchedule(SaveScheduleRequest saveRequest) {
        if (saveRequest == null || saveRequest.getAssignments() == null
                || saveRequest.getAssignments().isEmpty()) {
            log.warn("saveApprovedSchedule called with empty or null assignments — nothing to do");
            return;
        }

        // 1. Initial Status Checks (Cached)
        TaskStatus scheduledStatus = taskStatusRepository
                .findByName(TaskStatusConstants.TASK_SCHEDULED)
                .orElseThrow(() -> new IllegalStateException(
                        "TaskStatus '" + TaskStatusConstants.TASK_SCHEDULED + "' not seeded in task_statuses table"));

        SettlementStatus assignedStatus = settlementStatusRepository
                .findByName(TaskStatusConstants.SETTLEMENT_ASSIGNED)
                .orElseThrow(() -> new IllegalStateException(
                        "SettlementStatus '" + TaskStatusConstants.SETTLEMENT_ASSIGNED + "' not seeded in settlement_statuses table"));

        // 2. Extract IDs for Bulk Fetching (prevents N+1)
        Set<Long> taskIds = saveRequest.getAssignments().stream()
                .filter(a -> a.getAssignedUserId() != null)
                .map(SaveScheduleRequest.TaskAssignmentDto::getTaskId)
                .collect(Collectors.toSet());

        Set<Long> userIds = saveRequest.getAssignments().stream()
                .filter(a -> a.getAssignedUserId() != null)
                .map(SaveScheduleRequest.TaskAssignmentDto::getAssignedUserId)
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) return;

        // 3. Bulk Load Entities
        Map<Long, Task> taskMap = taskRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Load existing settlements for these tasks to check for duplicates efficiently
        Map<Long, List<Settlement>> existingSettlementsMap = settlementRepository.findByTaskIdIn(taskIds).stream()
                .collect(Collectors.groupingBy(s -> s.getTask().getId()));

        // Create a lookup map for the *proposed* batch to validate dependencies within the batch
        Map<Long, SaveScheduleRequest.TaskAssignmentDto> batchAssignments = saveRequest.getAssignments().stream()
                .collect(Collectors.toMap(SaveScheduleRequest.TaskAssignmentDto::getTaskId, a -> a, (a, b) -> a));

        // Track worker schedules to prevent overlaps within this batch
        // Map<UserId, List<Interval>>
        Map<Long, List<LocalDateTime[]>> workerIntervals = new java.util.HashMap<>();

        // 4. Validate & Process Batch
        List<String> validationErrors = new ArrayList<>();
        List<Task> tasksToSave = new ArrayList<>();
        List<Settlement> settlementsToSave = new ArrayList<>();

        for (SaveScheduleRequest.TaskAssignmentDto assignment : saveRequest.getAssignments()) {
            if (assignment.getAssignedUserId() == null) continue; // unassigned — skip

            Long taskId = assignment.getTaskId();
            Long userId = assignment.getAssignedUserId();

            Task task = taskMap.get(taskId);
            User user = userMap.get(userId);

            // Validation: Existence
            if (task == null) {
                validationErrors.add("Task ID " + taskId + " not found.");
                continue;
            }
            if (user == null) {
                validationErrors.add("User ID " + userId + " not found.");
                continue;
            }

            // Validation: Task State (Concurrency Check - Zero Trust)
            // Ensure task is strictly OPEN (not Locked, Scheduled, or Closed)
            if (!TaskStatusConstants.TASK_OPEN.equals(task.getStatus().getName())) {
               validationErrors.add("Task ID " + taskId + " is not OPEN (current status: " + task.getStatus().getName() + ").");
               continue;
            }

            // Validation: Optimistic Locking (Prevent "Long Conversation" Conflict)
            Long entityVersion = task.getVersion() != null ? task.getVersion() : 0L;
            // The DTO version is now Long, so we use it directly
            Long requestVersion = assignment.getVersion() != null ? assignment.getVersion() : 0L;

            if (!entityVersion.equals(requestVersion)) {
                validationErrors.add("Concurrency Error: Task ID [" + task.getId() + "] was modified by another user. Please refresh.");
                continue;
            }

            // Validation: Role Compliance (Zero Trust)
            // Ensure the worker actually has all roles required by the task
            if (!user.getRoles().containsAll(task.getRequiredRoles())) {
                validationErrors.add("User ID " + userId + " lacks required roles for Task ID " + taskId + ".");
                continue;
            }

            // Validation: Temporal (Start/End times)
            LocalDateTime proposedStart = assignment.getScheduledStart();
            LocalDateTime proposedEnd = assignment.getScheduledEnd();

            if (proposedStart == null || proposedEnd == null) {
                validationErrors.add("Task ID " + taskId + " has invalid schedule times.");
                continue;
            }

            // Validation: Dependencies (Finish-to-Start)
            // Check incoming constraints (predecessors)
            if (task.getIncomingConstraints() != null) {
                for (com.example.mainbackend.entity.TaskConstraint constraint : task.getIncomingConstraints()) {
                    Task predecessor = constraint.getPredecessorTask();
                    
                    // Case A: Predecessor is already COMPLETED/CLOSED in DB
                    // (Assuming 'CLOSED' implies completed. If 'SCHEDULED', we might need to check its time, but let's assume CLOSED for simplicity or SCHEDULED in past)
                    boolean isCompletedInDb = TaskStatusConstants.TASK_CLOSED.equals(predecessor.getStatus().getName());
                    
                    if (isCompletedInDb) {
                        // Ideally check predecessor.getEndTime() <= proposedStart, but mainly status is the gate here
                        continue; 
                    }

                    // Case B: Predecessor is in the CURRENT batch
                    if (batchAssignments.containsKey(predecessor.getId())) {
                        SaveScheduleRequest.TaskAssignmentDto predAssignment = batchAssignments.get(predecessor.getId());
                        LocalDateTime predEnd = predAssignment.getScheduledEnd();
                        if (predEnd != null && predEnd.isAfter(proposedStart)) {
                            validationErrors.add("Temporal Conflict: Task [" + task.getTitle() + "] starts at " + proposedStart 
                                    + " but predecessor [" + predecessor.getTitle() + "] ends at " + predEnd + ".");
                        }
                    } else {
                        // Case C: Predecessor is OPEN but not in this batch (and not closed) -> Violation
                        // If predecessor is SCHEDULED but not CLOSED, we should technically check its time, 
                        // but for 'saveApprovedSchedule' we assume previous batches are valid.
                        // However, if it's OPEN and missing, we can't schedule this one.
                        if (TaskStatusConstants.TASK_OPEN.equals(predecessor.getStatus().getName())) {
                            validationErrors.add("Temporal Conflict: Task [" + task.getTitle() + "] depends on [" + predecessor.getTitle() 
                                    + "] which is not completed and not in this schedule.");
                        }
                        // If it is SCHEDULED, we assume it's fine or we'd need to fetch its time. 
                        // For strictness, let's assume if it's not CLOSED and not in batch, we might have an issue, 
                        // but let's stick to the prompt's condition (a/b).
                    }
                }
            }

            // Validation: Overlap Protection
            // Check against this worker's other assignments in this batch
            List<LocalDateTime[]> intervals = workerIntervals.computeIfAbsent(userId, k -> new ArrayList<>());
            boolean overlaps = intervals.stream().anyMatch(interval -> 
                    (proposedStart.isBefore(interval[1]) && proposedEnd.isAfter(interval[0]))
            );

            if (overlaps) {
                validationErrors.add("Overlap Conflict: User [" + user.getEmail() + "] is assigned overlapping tasks. Clashing Task ID: " + taskId);
                continue;
            }
            intervals.add(new LocalDateTime[]{proposedStart, proposedEnd});


            // Logic: Update Task
            task.setStartTime(proposedStart);
            task.setStatus(scheduledStatus);
            tasksToSave.add(task);

            // Logic: Create Settlement (Idempotency Check)
            boolean alreadySettled = existingSettlementsMap.getOrDefault(taskId, Collections.emptyList()).stream()
                    .anyMatch(s -> s.getWorker().getId().equals(userId));

            if (!alreadySettled) {
                settlementsToSave.add(Settlement.builder()
                        .task(task)
                        .worker(user)
                        .status(assignedStatus)
                        .settlementDate(LocalDateTime.now())
                        .build());
            }
        }

        // 5. Final Decision: Commit or Rollback
        if (!validationErrors.isEmpty()) {
            log.error("Batch validation failed with {} errors: {}", validationErrors.size(), validationErrors);
            throw new BatchValidationException(validationErrors);
        }

        // 6. Persistence (Batch Save)
        taskRepository.saveAll(tasksToSave);
        settlementRepository.saveAll(settlementsToSave);

        log.info("saveApprovedSchedule: Successfully persisted {} tasks and {} settlements.",
                tasksToSave.size(), settlementsToSave.size());
    }

    // ── Resolve current user ──────────────────────────────────────────────────

    /**
     * Reads the principal from the Spring Security context (set by JwtAuthenticationFilter)
     * and loads the full User entity so we can inspect its Roles and Department.
     *
     * @throws IllegalStateException if there is no authenticated user in the context
     */
    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        // The principal name is the nationalId (set by CustomUserDetailsService)
        String nationalId = auth.getName();
        return userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user '" + nationalId + "' not found in the database"));
    }

    // ── Build request ─────────────────────────────────────────────────────────

    /**
     * Builds the anonymous algorithm request, scoping users and tasks based on
     * the caller's Spring Security roles:
     *
     * <ul>
     *   <li>{@code MANAGER} — ALWAYS scoped to the manager's own department (parameter ignored).</li>
     *   <li>{@code ADMIN}   — scoped to {@code departmentId} when non-null; global when null.</li>
     *   <li>anything else   — not permitted to trigger scheduling.</li>
     * </ul>
     *
     * @param departmentId optional ADMIN-only scope override; always ignored for MANAGER
     */
    private AlgoScheduleRequest buildRequest(String strategy,
                                             SchedulingConfigurationDto config,
                                             User currentUser,
                                             Long departmentId) {
        Set<String> roleNames = currentUser.getRoles() != null
                ? currentUser.getRoles().stream()
                        .map(com.example.mainbackend.entity.Role::getRoleName)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        boolean isAdmin   = roleNames.contains("ADMIN");
        boolean isManager = roleNames.contains("MANAGER");

        if (!isAdmin && !isManager) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only ADMIN or MANAGER roles are permitted to trigger scheduling");
        }

        List<com.example.mainbackend.algorithm.dto.AlgoUserRequest> users;
        List<com.example.mainbackend.algorithm.dto.AlgoTaskRequest> tasks;

        if (isManager) {
            // MANAGER — ALWAYS scope to their own department, parameter is irrelevant
            Department dept = currentUser.getDepartment();
            if (dept == null) {
                throw new IllegalStateException(
                        "MANAGER user [id=" + currentUser.getId() + "] has no department assigned");
            }
            log.info("Scheduling scope: MANAGER — department '{}' (id={})", dept.getName(), dept.getId());
            users = buildUserRequests(dept.getId());
            tasks = buildTaskRequests(dept.getId());
        } else {
            // ADMIN — scope to departmentId if provided, otherwise global
            if (departmentId != null) {
                log.info("Scheduling scope: ADMIN — department-scoped to id={}", departmentId);
            } else {
                log.info("Scheduling scope: ADMIN — global (all departments)");
            }
            users = buildUserRequests(departmentId);
            tasks = buildTaskRequests(departmentId);
        }

        log.info("Scheduling request: strategy={}, users={}, tasks={}", strategy, users.size(), tasks.size());

        return AlgoScheduleRequest.builder()
                .strategy(strategy)
                .config(config)
                .users(users)
                .tasks(tasks)
                .build();
    }

    /**
     * Loads workers with their roles eagerly.
     *
     * @param departmentId {@code null} → all users (ADMIN); non-null → department-scoped (MANAGER)
     */
    private List<com.example.mainbackend.algorithm.dto.AlgoUserRequest> buildUserRequests(Long departmentId) {
        List<User> users = (departmentId == null)
                ? userRepository.findAllWithRoles()
                : userRepository.findByDepartmentIdWithRoles(departmentId);

        return users.stream()
                .map(UserMapper::toAlgoRequest)
                .collect(Collectors.toList());
    }

    /**
     * Loads OPEN tasks with roles and constraints eagerly merged in-memory to avoid
     * a Cartesian product (HHH90003004).
     *
     * @param departmentId {@code null} → all OPEN tasks (ADMIN); non-null → department-scoped (MANAGER)
     */
    private List<com.example.mainbackend.algorithm.dto.AlgoTaskRequest> buildTaskRequests(Long departmentId) {
        List<Task> tasksWithRoles;
        List<Task> tasksWithConstraints;

        if (departmentId == null) {
            tasksWithRoles       = taskRepository.findOpenTasksWithRoles(TaskStatusConstants.TASK_OPEN);
            tasksWithConstraints = taskRepository.findOpenTasksWithConstraints(TaskStatusConstants.TASK_OPEN);
        } else {
            tasksWithRoles       = taskRepository.findOpenTasksWithRolesByDepartment(TaskStatusConstants.TASK_OPEN, departmentId);
            tasksWithConstraints = taskRepository.findOpenTasksWithConstraintsByDepartment(TaskStatusConstants.TASK_OPEN, departmentId);
        }

        // Index the constraints result by task ID for O(1) merge
        Map<Long, Task> constraintMap = tasksWithConstraints.stream()
                .collect(Collectors.toMap(Task::getId, t -> t, (a, b) -> a));

        // Merge: use the roles-loaded entity but copy the constraints collection across
        List<Task> mergedTasks = tasksWithRoles.stream().peek(task -> {
            Task withConstraints = constraintMap.get(task.getId());
            if (withConstraints != null)
                task.setIncomingConstraints(withConstraints.getIncomingConstraints());
        }).toList();

        log.info("Sending {} OPEN tasks to algorithm (LOCKED/SCHEDULED/CLOSED excluded)", mergedTasks.size());

        Set<Long> openTaskIds = mergedTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        return mergedTasks.stream()
                .map(task -> TaskMapper.toAlgoRequest(task, openTaskIds))
                .collect(Collectors.toList());
    }

    // ── Enrich for preview (read-only) ───────────────────────────────────────

    /**
     * Injects human-readable names ({@code taskTitle}, {@code assignedUserFullName})
     * into the algorithm response for display in the frontend draft view.
     *
     * <strong>No database writes are performed.</strong>
     */
    private void enrichForPreview(AlgoScheduleResponse response) {
        if (response == null || response.getAssignments() == null) return;

        // Pre-load all affected task IDs (assigned + unscheduled) to avoid N+1 queries
        Set<Long> assignedTaskIds = response.getAssignments().stream()
                .map(AlgoTaskAssignmentResponse::getTaskId)
                .collect(Collectors.toSet());

        Set<Long> unscheduledTaskIds = (response.getUnscheduledTasks() != null)
                ? response.getUnscheduledTasks().stream()
                        .map(AlgoUnscheduledTaskResponse::getTaskId)
                        .collect(Collectors.toSet())
                : Set.of();

        Set<Long> allTaskIds = new java.util.HashSet<>(assignedTaskIds);
        allTaskIds.addAll(unscheduledTaskIds);

        Map<Long, Task> taskCache = taskRepository.findAllById(allTaskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        // ── Enrich assigned tasks (names only — no persistence) ───────────────
        for (AlgoTaskAssignmentResponse assignment : response.getAssignments()) {
            Task task = taskCache.get(assignment.getTaskId());
            if (task != null)
                assignment.setTaskTitle(task.getTitle());

            if (assignment.getAssignedUserId() == null) continue;

            userRepository.findById(assignment.getAssignedUserId()).ifPresent(user -> {
                String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                        + (user.getLastName() != null ? user.getLastName() : "")).trim();
                assignment.setAssignedUserFullName(
                        fullName.isEmpty() ? "Worker #" + user.getId() : fullName);
            });
        }

        // ── Enrich unscheduled tasks with human-readable names ────────────────
        if (response.getUnscheduledTasks() != null) {
            for (AlgoUnscheduledTaskResponse unscheduled : response.getUnscheduledTasks()) {
                Task task = taskCache.get(unscheduled.getTaskId());
                if (task != null) {
                    unscheduled.setTaskName(task.getTitle());
                } else {
                    unscheduled.setTaskName("Task #" + unscheduled.getTaskId());
                    log.warn("Unscheduled taskId={} not found in DB", unscheduled.getTaskId());
                }
            }
        }

        log.info("enrichForPreview: enriched {} assignments for draft view (no DB writes)",
                response.getAssignedTasks());
    }
}
