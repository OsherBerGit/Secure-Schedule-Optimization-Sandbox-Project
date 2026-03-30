package com.example.mainbackend.algorithm.service;

import com.example.mainbackend.algorithm.AlgorithmClient;
import com.example.mainbackend.algorithm.dto.*;
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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *  - Zero-Trust — only IDs and capacity data leave this service to the algorithm
 *  - Mapper Pattern — entity-to-DTO conversion delegated to TaskMapper / UserMapper
 *  - N+1 prevention — JOIN FETCH queries used for tasks and users
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulingService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SettlementRepository settlementRepository;
    private final SchedulingConfigurationService configService;
    private final AlgorithmClient algorithmClient;
    private final TaskStatusRepository taskStatusRepository;
    private final SettlementStatusRepository settlementStatusRepository;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;


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
    public AlgoScheduleResponse runScheduling(String strategy, Long departmentId, Long configId, String nationalId) {
        log.info("User {} is requesting a schedule preview using strategy: {}", nationalId, strategy);

        User currentUser = userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + nationalId));


        SchedulingConfigurationDto config = configService.getConfigurationById(configId, nationalId);

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
    public AlgoScheduleResponse runScheduling(String strategy, Long departmentId, String nationalId) {
        return runScheduling(strategy, departmentId, null, nationalId);
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
    public void saveApprovedSchedule(SaveScheduleRequest saveRequest, String nationalId) {
        if (saveRequest == null || saveRequest.getAssignments() == null || saveRequest.getAssignments().isEmpty()) {
            log.info("Manager {} is saving an approved schedule batch", nationalId);
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
                        "SettlementStatus 'ASSIGNED' not seeded in settlement_statuses table"));

        // 2. Extract IDs for Bulk Fetching (prevents N+1)
        Set<Long> taskIds = saveRequest.getAssignments().stream()
                .filter(a -> a.getAssignedUserId() != null)
                .map(SaveScheduleRequest.TaskAssignmentDto::getTaskId)
                .collect(Collectors.toSet());

        Set<Long> userIds = saveRequest.getAssignments().stream()
                .filter(a -> a.getAssignedUserId() != null)
                .map(SaveScheduleRequest.TaskAssignmentDto::getAssignedUserId)
                .filter(java.util.Objects::nonNull) // Ensure no null user IDs
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
        Map<Long, List<LocalDateTime[]>> workerIntervals = new java.util.HashMap<>();

        // 4. Validate & Process Batch
        List<String> validationErrors = new ArrayList<>();
        List<Task> tasksToSave = new ArrayList<>();
        List<Settlement> settlementsToSave = new ArrayList<>();

        for (SaveScheduleRequest.TaskAssignmentDto assignment : saveRequest.getAssignments()) {
            if (assignment.getAssignedUserId() == null) {
                log.debug("Skipping unassigned task ID: {}", assignment.getTaskId());
                continue;
            }

            Long taskId = assignment.getTaskId();
            Long userId = assignment.getAssignedUserId();

            Task task = taskMap.get(taskId);
            User user = userMap.get(userId);
            
            // Accumulate errors for this assignment
            List<String> currentAssignmentErrors = new ArrayList<>();

            // Basic Validations
            currentAssignmentErrors.addAll(validateAssignmentBasic(assignment, task, user));

            if (!currentAssignmentErrors.isEmpty()) {
                validationErrors.addAll(currentAssignmentErrors);
                continue; // Move to next assignment if basic validation fails
            }

            // Temporal and Dependency Validations (only if basic checks pass)
            currentAssignmentErrors.addAll(validateTemporalAndDependencies(assignment, task, batchAssignments));

            if (!currentAssignmentErrors.isEmpty()) {
                validationErrors.addAll(currentAssignmentErrors);
                continue; // Move to next assignment if temporal/dependency validation fails
            }

            // Overlap Protection (only if previous checks pass)
            currentAssignmentErrors.addAll(validateWorkerOverlap(userId, assignment, workerIntervals, user));

            if (!currentAssignmentErrors.isEmpty()) {
                validationErrors.addAll(currentAssignmentErrors);
                continue; // Move to next assignment if overlap validation fails
            }

            // If all validations pass, prepare entities for saving
            prepareEntitiesForSave(assignment, task, user, scheduledStatus, assignedStatus, existingSettlementsMap, tasksToSave, settlementsToSave);
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

    // ── Private Helper Methods for saveApprovedSchedule ───────────────────────

    private List<String> validateAssignmentBasic(SaveScheduleRequest.TaskAssignmentDto assignment,
                                                 Task task,
                                                 User user) {
        List<String> errors = new ArrayList<>();
        Long taskId = assignment.getTaskId();
        Long userId = assignment.getAssignedUserId();

        if (task == null) {
            errors.add("Task ID " + taskId + " not found.");
            return errors;
        }
        if (user == null) {
            errors.add("User ID " + userId + " not found.");
            return errors;
        }

        if (!TaskStatusConstants.TASK_OPEN.equals(task.getStatus().getName()))
            errors.add("Task ID " + taskId + " is not OPEN (current status: " + task.getStatus().getName() + ").");

        Long entityVersion = task.getVersion() != null ? task.getVersion() : 0L;
        Long requestVersion = assignment.getVersion() != null ? assignment.getVersion() : 0L;

        if (!entityVersion.equals(requestVersion))
            errors.add("Concurrency Error: Task ID [" + task.getId() + "] was modified by another user. Please refresh.");

        if (task.getRequiredJob() != null && !user.getJobs().contains(task.getRequiredJob()))
            errors.add("User ID " + userId + " lacks required jobs for Task ID " + taskId + ".");

        LocalDateTime proposedStart = assignment.getScheduledStart();
        LocalDateTime proposedEnd = assignment.getScheduledEnd();

        if (proposedStart == null || proposedEnd == null)
            errors.add("Task ID " + taskId + " has invalid schedule times (start or end is null).");
        else if (proposedStart.isAfter(proposedEnd))
            errors.add("Task ID " + taskId + " has an invalid schedule: start time (" + proposedStart + ") is after end time (" + proposedEnd + ").");

        return errors;
    }

    private List<String> validateTemporalAndDependencies(SaveScheduleRequest.TaskAssignmentDto assignment,
                                                        Task task,
                                                        Map<Long, SaveScheduleRequest.TaskAssignmentDto> batchAssignments) {
        List<String> errors = new ArrayList<>();
        LocalDateTime proposedStart = assignment.getScheduledStart();
        LocalDateTime proposedEnd = assignment.getScheduledEnd();

        if (task.getIncomingConstraints() == null || task.getIncomingConstraints().isEmpty()) return errors;

        for (com.example.mainbackend.entity.TaskConstraint constraint : task.getIncomingConstraints()) {
            Task predecessor = constraint.getPredecessorTask();
            if (predecessor == null) continue;

            if (TaskStatusConstants.TASK_CLOSED.equals(predecessor.getStatus().getName()))
                continue; // Valid per rule: if closed in DB, consider it implicitly valid.

            if (batchAssignments.containsKey(predecessor.getId())) {
                SaveScheduleRequest.TaskAssignmentDto predAssignment = batchAssignments.get(predecessor.getId());
                LocalDateTime predStart = predAssignment.getScheduledStart();
                LocalDateTime predEnd = predAssignment.getScheduledEnd();

                if (predStart == null || predEnd == null) continue;

                String type = (constraint.getConstraintType() != null) ? constraint.getConstraintType().getName() : "FINISH_TO_START";

                switch (type) {
                    case "FINISH_TO_START" -> {
                        if (predEnd.isAfter(proposedStart))
                            errors.add("Temporal Conflict (FS): " + task.getTitle() + " must start after " + predecessor.getTitle() + " ends.");
                    }
                    case "START_TO_START" -> {
                        if (predStart.isAfter(proposedStart))
                            errors.add("Temporal Conflict (SS): " + task.getTitle() + " cannot start before " + predecessor.getTitle() + " starts.");
                    }
                    case "FINISH_TO_FINISH" -> {
                        if (predEnd.isAfter(proposedEnd))
                            errors.add("Temporal Conflict (FF): " + task.getTitle() + " cannot finish before " + predecessor.getTitle() + " finishes.");
                    }
                    case "START_TO_FINISH" -> {
                        if (predStart.isAfter(proposedEnd))
                            errors.add("Temporal Conflict (SF): " + task.getTitle() + " cannot finish before " + predecessor.getTitle() + " starts.");
                    }
                }
            } else
                errors.add("Dependency Error: Task [" + task.getTitle() + "] depends on [" + predecessor.getTitle() + "] which is missing from this batch.");
        }

        return errors;
    }

    private List<String> validateWorkerOverlap(Long userId,
                                               SaveScheduleRequest.TaskAssignmentDto assignment,
                                               Map<Long, List<LocalDateTime[]>> workerIntervals,
                                               User user) {
        List<String> errors = new ArrayList<>();
        LocalDateTime proposedStart = assignment.getScheduledStart();
        LocalDateTime proposedEnd = assignment.getScheduledEnd();

        List<LocalDateTime[]> intervals = workerIntervals.computeIfAbsent(userId, k -> new ArrayList<>());
        boolean overlaps = intervals.stream().anyMatch(interval ->
                (proposedStart.isBefore(interval[1]) && proposedEnd.isAfter(interval[0]))
        );

        if (overlaps)
            errors.add("Overlap Conflict: User [" + user.getEmail() + "] is assigned overlapping tasks. Proposed interval: [" + proposedStart + " - " + proposedEnd + "].");
        else
            intervals.add(new LocalDateTime[]{proposedStart, proposedEnd});

        return errors;
    }

    private void prepareEntitiesForSave(SaveScheduleRequest.TaskAssignmentDto assignment,
                                        Task task,
                                        User user,
                                        TaskStatus scheduledStatus,
                                        SettlementStatus assignedStatus,
                                        Map<Long, List<Settlement>> existingSettlementsMap,
                                        List<Task> tasksToSave,
                                        List<Settlement> settlementsToSave) {
        
        task.setVersion(assignment.getVersion()); // Set version for optimistic locking
        
        task.setStartTime(assignment.getScheduledStart());
        task.setStatus(scheduledStatus);
        tasksToSave.add(task);

        boolean alreadySettled = existingSettlementsMap.getOrDefault(task.getId(), Collections.emptyList()).stream()
                .anyMatch(s -> s.getWorker() != null && s.getWorker().getId().equals(user.getId()));

        if (!alreadySettled)
            settlementsToSave.add(Settlement.builder()
                    .task(task)
                    .worker(user)
                    .status(assignedStatus)
                    .settlementDate(LocalDateTime.now())
                    .build());
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
        String roleName = currentUser.getRole().getRoleName();

        boolean isAdmin   = "ADMIN".equals(roleName);
        boolean isManager = "MANAGER".equals(roleName);

        if (!isAdmin && !isManager)
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only ADMIN or MANAGER roles are permitted to trigger scheduling");

        List<AlgoUserRequest> users;
        List<AlgoTaskRequest> tasks;

        if (isManager) {
            // MANAGER — ALWAYS scope to their own department, parameter is irrelevant
            Department dept = currentUser.getDepartment();
            if (dept == null)
                throw new IllegalStateException(
                        "MANAGER user [id=" + currentUser.getId() + "] has no department assigned");

            log.info("Scheduling scope: MANAGER — department '{}' (id={})", dept.getName(), dept.getId());
            users = buildUserRequests(dept.getId());
            tasks = buildTaskRequests(dept.getId());
        } else {
            // ADMIN — scope to departmentId if provided, otherwise global
            if (departmentId != null)
                log.info("Scheduling scope: ADMIN — department-scoped to id={}", departmentId);
            else
                log.info("Scheduling scope: ADMIN — global (all departments)");

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
    private List<AlgoUserRequest> buildUserRequests(Long departmentId) {
        List<User> users = (departmentId == null)
                ? userRepository.findAllWithJobs()
                : userRepository.findAllWithJobsByDepartment(departmentId);

        return users.stream()
                .map(userMapper::toAlgoRequest)
                .collect(Collectors.toList());
    }

    /**
     * Loads OPEN tasks with roles and constraints eagerly merged in-memory to avoid
     * a Cartesian product (HHH90003004).
     *
     * @param departmentId {@code null} → all OPEN tasks (ADMIN); non-null → department-scoped (MANAGER)
     */
    private List<AlgoTaskRequest> buildTaskRequests(Long departmentId) {
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
                .map(task -> taskMapper.toAlgoRequest(task, openTaskIds))
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

        Set<Long> userIds = response.getAssignments().stream()
                .map(AlgoTaskAssignmentResponse::getAssignedUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userCache = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // ── Enrich assigned tasks (names only — no persistence) ───────────────
        for (AlgoTaskAssignmentResponse assignment : response.getAssignments()) {
            Task task = taskCache.get(assignment.getTaskId());
            if (task != null)
                assignment.setTaskTitle(task.getTitle());

            if (assignment.getAssignedUserId() == null) continue;

            User user = userCache.get(assignment.getAssignedUserId());
            if (user != null) {
                String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                        + (user.getLastName() != null ? user.getLastName() : "")).trim();
                assignment.setAssignedUserFullName(
                        fullName.isEmpty() ? "Worker #" + user.getId() : fullName);
            }
        }

        // ── Enrich unscheduled tasks with human-readable names ────────────────
        if (response.getUnscheduledTasks() != null) {
            for (AlgoUnscheduledTaskResponse unscheduled : response.getUnscheduledTasks()) {
                Task task = taskCache.get(unscheduled.getTaskId());
                if (task != null)
                    unscheduled.setTaskName(task.getTitle());
                else {
                    unscheduled.setTaskName("Task #" + unscheduled.getTaskId());
                    log.warn("Unscheduled taskId={} not found in DB", unscheduled.getTaskId());
                }
            }
        }

        log.info("enrichForPreview: enriched {} assignments for draft view (no DB writes)",
                response.getAssignedTasks());
    }
}
