package com.example.mainbackend.service;

import com.example.mainbackend.constants.TaskStatusLevel;
import com.example.mainbackend.dto.task.TaskCreateRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.entity.*;
import com.example.mainbackend.mapper.TaskMapper;
import com.example.mainbackend.repository.*;
import com.example.mainbackend.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskPriorityRepository taskPriorityRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final SettlementRepository settlementRepository;
    private final TaskConstraintRepository taskConstraintRepository;
    private final SkillRepository skillRepository; // Added dependency
    private final TaskMapper taskMapper;
    private final SecurityHelper securityHelper;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public TaskResponseDto createTask(TaskCreateRequest request) {
        Task task = buildTaskFromRequest(request);

        // Department Access Control
        if (securityHelper.isManager()) {
            Long managerDeptId = securityHelper.getCurrentUserDepartmentId();
            if (request.getDepartmentId() != null && !request.getDepartmentId().equals(managerDeptId))
                throw new AccessDeniedException("Managers cannot create tasks for other departments.");

            // Enforce Manager's department
            task.setDepartment(departmentRepository.getReferenceById(managerDeptId));
        } else if (securityHelper.isAdmin()) {
            // ADMIN can assign any department
            if (request.getDepartmentId() != null) {
                Department dept = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));
                task.setDepartment(dept);
            }
        }

        return taskMapper.toDto(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getAllTasks() {
        if (securityHelper.isManager()) {
            Long deptId = securityHelper.getCurrentUserDepartmentId();
            return taskRepository.findAllByDepartmentId(deptId).stream()
                    .map(taskMapper::toDto)
                    .collect(Collectors.toList());
        }
        return taskRepository.findAll().stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<TaskResponseDto> getTaskById(Long id) {
        return taskRepository.findById(id).map(taskMapper::toDto);
    }

    @Transactional
    public Optional<TaskResponseDto> updateTask(Long id, TaskCreateRequest request) {
        return taskRepository.findById(id).map(existing -> {

            boolean requiresStatusReset = false;
            // 1. Check if requiredSkill is changed
            if (request.getRequiredSkill() != null
                    && existing.getRequiredSkill() != null
                    && !request.getRequiredSkill().equals(existing.getRequiredSkill().getId())) {
                List<Settlement> settlements = settlementRepository.findByTaskId(id);
                if (!settlements.isEmpty()) {
                    settlementRepository.deleteAll(settlements);
                    requiresStatusReset = true;
                }
            }

            // 2. Resolve the new status
            TaskStatus newStatus = existing.getStatus();
            if (requiresStatusReset) {
                newStatus = taskStatusRepository.findByName(TaskStatusLevel.OPEN.name())
                        .orElseThrow(() -> new IllegalStateException("OPEN status not seeded"));
            } else if (request.getStatusId() != null && !request.getStatusId().equals(existing.getStatus().getId())) {
                newStatus = taskStatusRepository.findById(request.getStatusId())
                        .orElseThrow(() -> new IllegalArgumentException("TaskStatus not found: " + request.getStatusId()));

                // If moving to LOCKED, drop settlements
                if (newStatus.getName().equals(TaskStatusLevel.LOCKED.name())) {
                    List<Settlement> settlements = settlementRepository.findByTaskId(id);
                    if (!settlements.isEmpty()) {
                        settlementRepository.deleteAll(settlements);
                    }
                }
            }

            // Update existing entity manually
            existing.setTitle(request.getTitle());
            existing.setDescription(request.getDescription());
            existing.setDeadline(request.getDeadline());
            existing.setDurationHours(request.getDurationHours());

            TaskPriority priority = taskPriorityRepository.findById(request.getPriorityId())
                    .orElseThrow(() -> new RuntimeException("Priority not found: " + request.getPriorityId()));
            existing.setPriority(priority);
            existing.setStatus(newStatus);

            if (request.getRequiredSkill() != null) {
                Skill skill = skillRepository.findById(request.getRequiredSkill())
                        .orElseThrow(() -> new IllegalArgumentException("skill not found: " + request.getRequiredSkill()));
                existing.setRequiredSkill(skill);
            } else {
                existing.setRequiredSkill(null);
            }

            return taskMapper.toDto(taskRepository.save(existing));
        });
    }

    @Transactional
    public boolean deleteTask(Long id) {
        if (!taskRepository.existsById(id)) return false;

        // Delete settlements first to prevent FK constraint violation
        List<Settlement> settlements = settlementRepository.findByTaskId(id);
        if (!settlements.isEmpty())
            settlementRepository.deleteAll(settlements);

        // Delete constraints where this task is involved
        List<TaskConstraint> outConstraints = taskConstraintRepository.findByPredecessorTaskId(id);
        if (!outConstraints.isEmpty())
            taskConstraintRepository.deleteAll(outConstraints);

        List<TaskConstraint> inConstraints = taskConstraintRepository.findBySuccessorTaskId(id);
        if (!inConstraints.isEmpty())
            taskConstraintRepository.deleteAll(inConstraints);

        taskRepository.deleteById(id);
        return true;
    }

    /**
     * Returns all tasks assigned to a worker via their Settlements.
     * Assignment is now the single source of truth in the Settlement entity.
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksByWorkerId(Long workerId) {
        return settlementRepository.findByWorkerId(workerId).stream()
                .map(s -> taskMapper.toDto(s.getTask()))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns only OPEN tasks for the scheduling algorithm.
     * No category check needed â€” task_statuses table holds only task lifecycle statuses.
     */
    @Transactional(readOnly = true)
    public List<Task> getOpenTasksForScheduling() { return taskRepository.findByStatusName(TaskStatusLevel.OPEN.name()); }

    /**
     * Retrieves all tasks with a specific status.
     *
     * @param statusId the ID of the task status
     * @return list of TaskResponseDto
     */
    public List<TaskResponseDto> getTasksByStatusId(Long statusId) {
        return taskRepository.findByStatusId(statusId).stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    private Task buildTaskFromRequest(TaskCreateRequest request) {
        TaskPriority priority = taskPriorityRepository.findById(request.getPriorityId())
                .orElseThrow(() -> new RuntimeException("Priority not found: " + request.getPriorityId()));

        // Default status for new tasks is OPEN
        TaskStatus resolvedStatus;
        if (request.getStatusId() != null)
            resolvedStatus = taskStatusRepository.findById(request.getStatusId()).orElseThrow();
        else
            resolvedStatus = taskStatusRepository.findByName(TaskStatusLevel.OPEN.name())
                .orElseThrow(() -> new IllegalStateException("OPEN status not seeded in task_statuses"));

        Task.TaskBuilder builder = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .durationHours(request.getDurationHours())
                .priority(priority)
                .status(resolvedStatus);

        // Handle required skill
        if (request.getRequiredSkill() != null) {
            Skill skill = skillRepository.findById(request.getRequiredSkill())
                    .orElseThrow(() -> new IllegalArgumentException("skill not found: " + request.getRequiredSkill()));
            builder.requiredSkill(skill);
        } else
            builder.requiredSkill(null);


        return builder.build();
    }
}
