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
     */
    @Transactional(readOnly = true)
    public AlgoScheduleResponse runScheduling(String strategy, Long departmentId) {
        User currentUser = resolveCurrentUser();
        SchedulingConfigurationDto config = configService.getActiveConfiguration();
        AlgoScheduleRequest request = buildRequest(strategy, config, currentUser, departmentId);

        AlgoScheduleResponse response = algorithmClient.requestSchedule(request);

        // Enrich with human-readable names for the preview — no DB writes
        enrichForPreview(response);

        return response;
    }

    /**
     * PHASE 2 — Approve and Save.
     *
     * Persists the admin-approved draft assignments:
     *   - Task lifecycle  → SCHEDULED
     *   - Settlement exec → ASSIGNED  (idempotent guard against duplicates)
     *
     * Only assignments where {@code assignedUserId} is non-null are processed.
     * Unknown task or user IDs are logged and skipped gracefully.
     *
     * @param saveRequest the approved assignments forwarded from the frontend
     */
    @Transactional
    public void saveApprovedSchedule(SaveScheduleRequest saveRequest) {
        if (saveRequest == null || saveRequest.getAssignments() == null
                || saveRequest.getAssignments().isEmpty()) {
            log.warn("saveApprovedSchedule called with empty or null assignments — nothing to do");
            return;
        }

        TaskStatus scheduledStatus = taskStatusRepository
                .findByName(TaskStatusConstants.TASK_SCHEDULED)
                .orElseThrow(() -> new IllegalStateException(
                        "TaskStatus '" + TaskStatusConstants.TASK_SCHEDULED + "' not seeded in task_statuses table"));

        SettlementStatus assignedStatus = settlementStatusRepository
                .findByName(TaskStatusConstants.SETTLEMENT_ASSIGNED)
                .orElseThrow(() -> new IllegalStateException(
                        "SettlementStatus '" + TaskStatusConstants.SETTLEMENT_ASSIGNED + "' not seeded in settlement_statuses table"));

        // Pre-load all referenced task IDs in one query to avoid N+1
        Set<Long> taskIds = saveRequest.getAssignments().stream()
                .filter(a -> a.getAssignedUserId() != null)
                .map(SaveScheduleRequest.TaskAssignmentDto::getTaskId)
                .collect(Collectors.toSet());

        Map<Long, Task> taskCache = taskRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        int persisted = 0;
        for (SaveScheduleRequest.TaskAssignmentDto assignment : saveRequest.getAssignments()) {
            if (assignment.getAssignedUserId() == null) continue; // unassigned — skip

            Task task = taskCache.get(assignment.getTaskId());
            if (task == null) {
                log.warn("saveApprovedSchedule: unknown taskId={}, skipping", assignment.getTaskId());
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
                    log.debug("Saved: Task [{}] → SCHEDULED; Settlement → ASSIGNED for worker [{}]",
                            task.getId(), user.getId());
                } else {
                    log.debug("Skipping duplicate settlement for task [{}] / worker [{}]",
                            task.getId(), user.getId());
                }

            }, () -> log.warn("saveApprovedSchedule: unknown userId={}, skipping",
                    assignment.getAssignedUserId()));

            persisted++;
        }

        log.info("saveApprovedSchedule: {} assignments persisted (SCHEDULED + ASSIGNED)", persisted);
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
            if (withConstraints != null) {
                task.setIncomingConstraints(withConstraints.getIncomingConstraints());
            }
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
            if (task != null) {
                assignment.setTaskTitle(task.getTitle());
            }

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
