package com.example.mainbackend.algorithm.service;

import com.example.mainbackend.algorithm.AlgorithmClient;
import com.example.mainbackend.algorithm.dto.*;
import com.example.mainbackend.constants.RoleType;
import com.example.mainbackend.constants.SettlementStatusLevel;
import com.example.mainbackend.constants.TaskStatusLevel;
import com.example.mainbackend.entity.*;
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
 * Orchestrates the secure scheduling flow (Zero-Trust execution).
 * * Architecture Rules Enforced:
 * 1. Zero-Trust: The Algorithm engine is treated as an untrusted external entity.
 * All outputs must be strictly validated against the central DB state before persistence.
 * 2. Data Isolation: Only OPEN tasks are exposed. CLOSED/LOCKED tasks remain hidden.
 * 3. N+1 Prevention: Uses Bulk-fetching patterns to validate assignments efficiently.
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

    @Transactional(readOnly = true)
    public AlgoScheduleResponse runScheduling(String strategy, Long departmentId, Long configId, String nationalId) {
        log.info("User {} is requesting a schedule preview using strategy: {}", nationalId, strategy);

        User currentUser = userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + nationalId));

        SchedulingConfigurationDto config = configService.getConfigurationById(configId, nationalId);
        AlgoScheduleRequest request = buildRequest(strategy, config, currentUser, departmentId);

        AlgoScheduleResponse response = algorithmClient.requestSchedule(request);

        enrichForPreview(response);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveApprovedSchedule(SaveScheduleRequest saveRequest, String nationalId) {
        if (saveRequest == null || saveRequest.getAssignments() == null || saveRequest.getAssignments().isEmpty()) {
            log.info("Manager {} is saving an approved schedule batch", nationalId);
            return;
        }

        TaskStatus scheduledStatus = taskStatusRepository.findByName(TaskStatusLevel.SCHEDULED.name())
                .orElseThrow(() -> new IllegalStateException("TaskStatus '" + TaskStatusLevel.SCHEDULED.name() + "' not seeded in task_statuses table"));

        SettlementStatus assignedStatus = settlementStatusRepository.findByName(SettlementStatusLevel.ASSIGNED.name())
                .orElseThrow(() -> new IllegalStateException("SettlementStatus 'ASSIGNED' not seeded in settlement_statuses table"));

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

        // Security Gate: Bulk load to verify entities exist and to prevent N+1 queries
        Map<Long, Task> taskMap = taskRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, List<Settlement>> existingSettlementsMap = settlementRepository.findByTaskIdIn(taskIds).stream()
                .collect(Collectors.groupingBy(s -> s.getTask().getId()));

        Map<Long, SaveScheduleRequest.TaskAssignmentDto> batchAssignments = saveRequest.getAssignments().stream()
                .collect(Collectors.toMap(SaveScheduleRequest.TaskAssignmentDto::getTaskId, a -> a, (a, b) -> a));

        Map<Long, List<LocalDateTime[]>> userIntervals = new java.util.HashMap<>();
        List<String> validationErrors = new ArrayList<>();
        List<Task> tasksToSave = new ArrayList<>();
        List<Settlement> settlementsToSave = new ArrayList<>();

        for (SaveScheduleRequest.TaskAssignmentDto assignment : saveRequest.getAssignments()) {
            if (assignment.getAssignedUserId() == null) continue;

            Task task = taskMap.get(assignment.getTaskId());
            User user = userMap.get(assignment.getAssignedUserId());

            List<String> currentAssignmentErrors = new ArrayList<>();

            currentAssignmentErrors.addAll(validateAssignmentBasic(assignment, task, user));
            if (!currentAssignmentErrors.isEmpty()) {
                validationErrors.addAll(currentAssignmentErrors);
                continue;
            }

            currentAssignmentErrors.addAll(validateTemporalAndDependencies(assignment, task, batchAssignments));
            if (!currentAssignmentErrors.isEmpty()) {
                validationErrors.addAll(currentAssignmentErrors);
                continue;
            }

            currentAssignmentErrors.addAll(validateUserOverlap(user.getId(), assignment, userIntervals, user));
            if (!currentAssignmentErrors.isEmpty()) {
                validationErrors.addAll(currentAssignmentErrors);
                continue;
            }

            prepareEntitiesForSave(assignment, task, user, scheduledStatus, assignedStatus, existingSettlementsMap, tasksToSave, settlementsToSave);
        }

        if (!validationErrors.isEmpty()) {
            log.error("Batch validation failed with {} errors: {}", validationErrors.size(), validationErrors);
            throw new BatchValidationException(validationErrors);
        }

        taskRepository.saveAll(tasksToSave);
        settlementRepository.saveAll(settlementsToSave);

        log.info("saveApprovedSchedule: Successfully persisted {} tasks and {} settlements.", tasksToSave.size(), settlementsToSave.size());
    }

    private List<String> validateAssignmentBasic(SaveScheduleRequest.TaskAssignmentDto assignment,
                                                 Task task,
                                                 User user) {
        List<String> errors = new ArrayList<>();

        if (task == null) {
            errors.add(String.format("Security/Integrity Risk: Task ID %d not found in database.", assignment.getTaskId()));
            return errors;
        }
        if (user == null) {
            errors.add(String.format("Security/Integrity Risk: Assigned User ID %d not found in database.", assignment.getAssignedUserId()));
            return errors;
        }

        if (!TaskStatusLevel.OPEN.name().equals(task.getStatus().getName()))
            errors.add(String.format("State violation: Task ID %d is not OPEN (current: %s).", task.getId(), task.getStatus().getName()));

        Long entityVersion = task.getVersion() != null ? task.getVersion() : 0L;
        Long requestVersion = assignment.getVersion() != null ? assignment.getVersion() : 0L;

        if (!entityVersion.equals(requestVersion))
            errors.add(String.format("Concurrency violation: Task ID %d was modified externally. Requires refresh.", task.getId()));

        if (task.getRequiredSkills() != null && !task.getRequiredSkills().isEmpty() && !user.getSkills().containsAll(task.getRequiredSkills()))
            errors.add(String.format("Compliance violation: User %s lacks required skills for Task %d.", user.getEmail(), task.getId()));

        if (assignment.getScheduledStart() == null || assignment.getScheduledEnd() == null)
            errors.add(String.format("Data violation: Task %d schedule times cannot be null.", task.getId()));

        else if (assignment.getScheduledStart().isAfter(assignment.getScheduledEnd()))
            errors.add(String.format("Temporal violation: Task %d start time must precede end time.", task.getId()));

        return errors;
    }

    private List<String> validateTemporalAndDependencies(SaveScheduleRequest.TaskAssignmentDto assignment, Task task,
                                                        Map<Long, SaveScheduleRequest.TaskAssignmentDto> batchAssignments) {
        List<String> errors = new ArrayList<>();
        if (task.getIncomingConstraints() == null || task.getIncomingConstraints().isEmpty()) return errors;

        for (TaskConstraint constraint : task.getIncomingConstraints()) {
            Task predecessor = constraint.getPredecessorTask();
            if (predecessor == null) continue;

            if (TaskStatusLevel.CLOSED.name().equals(predecessor.getStatus().getName())) continue;

            if (batchAssignments.containsKey(predecessor.getId())) {
                SaveScheduleRequest.TaskAssignmentDto predAssignment = batchAssignments.get(predecessor.getId());
                LocalDateTime predStart = predAssignment.getScheduledStart();
                LocalDateTime predEnd = predAssignment.getScheduledEnd();

                if (predStart == null || predEnd == null) continue;

                String type = (constraint.getConstraintType() != null) ? constraint.getConstraintType().getName() : "FINISH_TO_START";

                switch (type) {
                    case "FINISH_TO_START" -> {
                        if (predEnd.isAfter(assignment.getScheduledStart()))
                            errors.add(String.format("Constraint (FS) failed: Task '%s' must start after '%s' ends.", task.getTitle(), predecessor.getTitle()));
                    }
                    case "START_TO_START" -> {
                        if (predStart.isAfter(assignment.getScheduledStart()))
                            errors.add(String.format("Constraint (SS) failed: Task '%s' cannot start before '%s' starts.", task.getTitle(), predecessor.getTitle()));
                    }
                    case "FINISH_TO_FINISH" -> {
                        if (predEnd.isAfter(assignment.getScheduledEnd()))
                            errors.add(String.format("Constraint (FF) failed: Task '%s' cannot finish before '%s' finishes.", task.getTitle(), predecessor.getTitle()));
                    }
                    case "START_TO_FINISH" -> {
                        if (predStart.isAfter(assignment.getScheduledEnd()))
                            errors.add(String.format("Constraint (SF) failed: Task '%s' cannot finish before '%s' starts.", task.getTitle(), predecessor.getTitle()));
                    }
                }
            } else
                errors.add(String.format("Dependency missing: Task '%s' requires '%s', which is absent from this batch.", task.getTitle(), predecessor.getTitle()));
        }

        return errors;
    }

    private List<String> validateUserOverlap(Long userId, SaveScheduleRequest.TaskAssignmentDto assignment, Map<Long, List<LocalDateTime[]>> userIntervals, User user) {
        List<String> errors = new ArrayList<>();
        List<LocalDateTime[]> intervals = userIntervals.computeIfAbsent(userId, k -> new ArrayList<>());

        boolean overlaps = intervals.stream().anyMatch(interval ->
                (assignment.getScheduledStart().isBefore(interval[1]) && assignment.getScheduledEnd().isAfter(interval[0])));

        if (overlaps)
            errors.add(String.format("Schedule overlap: User '%s' already has assignments during [%s - %s].", user.getEmail(), assignment.getScheduledStart(), assignment.getScheduledEnd()));
        else
            intervals.add(new LocalDateTime[]{assignment.getScheduledStart(), assignment.getScheduledEnd()});

        return errors;
    }

    private void prepareEntitiesForSave(SaveScheduleRequest.TaskAssignmentDto assignment, Task task, User user, TaskStatus scheduledStatus, SettlementStatus assignedStatus,
                                        Map<Long, List<Settlement>> existingSettlementsMap, List<Task> tasksToSave, List<Settlement> settlementsToSave) {

        task.setVersion(assignment.getVersion());
        task.setStartTime(assignment.getScheduledStart());
        task.setStatus(scheduledStatus);
        tasksToSave.add(task);

        boolean alreadySettled = existingSettlementsMap.getOrDefault(task.getId(), Collections.emptyList()).stream()
                .anyMatch(s -> s.getUser() != null && s.getUser().getId().equals(user.getId()));

        if (!alreadySettled)
            settlementsToSave.add(Settlement.builder()
                    .task(task)
                    .user(user)
                    .status(assignedStatus)
                    .settlementDate(LocalDateTime.now())
                    .build());
    }

    private AlgoScheduleRequest buildRequest(String strategy, SchedulingConfigurationDto config, User currentUser, Long departmentId) {
        String roleName = currentUser.getRole().getName();
        boolean isAdmin   = RoleType.ADMIN.name().equals(roleName);
        boolean isManager = RoleType.MANAGER.name().equals(roleName);

        if (!isAdmin && !isManager)
            throw new org.springframework.security.access.AccessDeniedException("Scheduling restricted to ADMIN or MANAGER roles.");

        List<AlgoUserRequest> users;
        List<AlgoTaskRequest> tasks;

        if (isManager) {
            Department dept = currentUser.getDepartment();
            if (dept == null) throw new IllegalStateException("MANAGER lacks assigned department context.");

            users = buildUserRequests(dept.getId());
            tasks = buildTaskRequests(dept.getId());
        } else {
            users = buildUserRequests(departmentId);
            tasks = buildTaskRequests(departmentId);
        }

        return AlgoScheduleRequest.builder()
                .strategy(strategy)
                .config(config)
                .users(users)
                .tasks(tasks)
                .build();
    }

    private List<AlgoUserRequest> buildUserRequests(Long departmentId) {
        List<User> users = (departmentId == null)
                ? userRepository.findAllWithSkills()
                : userRepository.findAllWithSkillsByDepartment(departmentId);

        if (users.isEmpty()) return Collections.emptyList();

        List<String> activeStatuses = List.of(SettlementStatusLevel.ASSIGNED.name(), SettlementStatusLevel.IN_PROGRESS.name());
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        Map<Long, Long> activeCountMap = settlementRepository.countActiveSettlementsUserIds(userIds, activeStatuses).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return users.stream()
                .map(user -> {
                    long activeCount = activeCountMap.getOrDefault(user.getId(), 0L);
                    int effectiveMaxTasks = Math.max(0, user.getMaxTasks() - (int) activeCount);
                    return userMapper.toAlgoRequest(user, effectiveMaxTasks);
                })
                .toList();
    }

    private List<AlgoTaskRequest> buildTaskRequests(Long departmentId) {
        List<Task> tasksWithRoles = (departmentId == null)
                ? taskRepository.findOpenTasksWithSkills(TaskStatusLevel.OPEN.name())
                : taskRepository.findOpenTasksWithSkillsByDepartment(TaskStatusLevel.OPEN.name(), departmentId);

        List<Task> tasksWithConstraints = (departmentId == null)
                ? taskRepository.findOpenTasksWithConstraints(TaskStatusLevel.OPEN.name())
                : taskRepository.findOpenTasksWithConstraintsByDepartment(TaskStatusLevel.OPEN.name(), departmentId);

        Map<Long, Task> constraintMap = tasksWithConstraints.stream()
                .collect(Collectors.toMap(Task::getId, t -> t, (a, b) -> a));

        List<Task> mergedTasks = tasksWithRoles.stream().peek(task -> {
            Task withConstraints = constraintMap.get(task.getId());
            if (withConstraints != null) task.setIncomingConstraints(withConstraints.getIncomingConstraints());
        }).toList();

        Set<Long> openTaskIds = mergedTasks.stream().map(Task::getId).collect(Collectors.toSet());

        return mergedTasks.stream()
                .map(task -> taskMapper.toAlgoRequest(task, openTaskIds))
                .toList();
    }

    private void enrichForPreview(AlgoScheduleResponse response) {
        if (response == null || response.getAssignments() == null) return;

        Set<Long> assignedTaskIds = response.getAssignments().stream().map(AlgoTaskAssignmentResponse::getTaskId).collect(Collectors.toSet());
        Set<Long> unscheduledTaskIds = (response.getUnscheduledTasks() != null)
                ? response.getUnscheduledTasks().stream().map(AlgoUnscheduledTaskResponse::getTaskId).collect(Collectors.toSet())
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

        for (AlgoTaskAssignmentResponse assignment : response.getAssignments()) {
            Task task = taskCache.get(assignment.getTaskId());
            if (task != null) assignment.setTaskTitle(task.getTitle());

            if (assignment.getAssignedUserId() == null) continue;

            User user = userCache.get(assignment.getAssignedUserId());
            if (user != null) {
                String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "")).trim();
                assignment.setAssignedUserFullName(fullName.isEmpty() ? "User #" + user.getId() : fullName);            }
        }

        if (response.getUnscheduledTasks() != null) {
            for (AlgoUnscheduledTaskResponse unscheduled : response.getUnscheduledTasks()) {
                Task task = taskCache.get(unscheduled.getTaskId());
                if (task != null) unscheduled.setTaskName(task.getTitle());
                else unscheduled.setTaskName("Task #" + unscheduled.getTaskId());
            }
        }
    }
}
