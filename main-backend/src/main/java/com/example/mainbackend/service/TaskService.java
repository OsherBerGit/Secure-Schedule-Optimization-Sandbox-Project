package com.example.mainbackend.service;

import com.example.mainbackend.constants.TaskStatusLevel;
import com.example.mainbackend.dto.task.TaskCreateRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.entity.*;
import com.example.mainbackend.mapper.TaskMapper;
import com.example.mainbackend.repository.*;
import com.example.mainbackend.security.SecurityHelper;
import com.example.mainbackend.util.CycleDetectionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final CycleDetectionUtil cycleDetectionUtil;

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
            
            // BUSINESS RULE: If the required skills for a task change, the currently assigned workers 
            // might no longer be qualified. We must delete existing assignments (settlements) 
            // and force the task back to the OPEN pool so the algorithm can reschedule it.
            if (request.getRequiredSkills() != null
                    && existing.getRequiredSkills() != null) {
                Set<Long> existingSkillIds = existing.getRequiredSkills().stream()
                        .map(Skill::getId)
                        .collect(Collectors.toSet());
                if (!request.getRequiredSkills().equals(existingSkillIds)) {
                    List<Settlement> settlements = settlementRepository.findByTaskId(id);
                    if (!settlements.isEmpty()) {
                        settlementRepository.deleteAll(settlements);
                        requiresStatusReset = true;
                    }
                }
            } else if ((request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty())
                    || (existing.getRequiredSkills() != null && !existing.getRequiredSkills().isEmpty())) {
                List<Settlement> settlements = settlementRepository.findByTaskId(id);
                if (!settlements.isEmpty()) {
                    settlementRepository.deleteAll(settlements);
                    requiresStatusReset = true;
                }
            }

            // 2. Resolve the new status
            TaskStatus newStatus = existing.getStatus();
            if (requiresStatusReset)
                newStatus = taskStatusRepository.findByName(TaskStatusLevel.OPEN.name())
                        .orElseThrow(() -> new IllegalStateException("OPEN status not seeded"));

            else if (request.getStatusId() != null && !request.getStatusId().equals(existing.getStatus().getId())) {
                newStatus = taskStatusRepository.findById(request.getStatusId())
                        .orElseThrow(() -> new IllegalArgumentException("TaskStatus not found: " + request.getStatusId()));

                // BUSINESS RULE: LOCKED status means the task is suspended or on hold.
                // We drop existing settlements to free up those workers for other tasks.
                if (newStatus.getName().equals(TaskStatusLevel.LOCKED.name())) {
                    List<Settlement> settlements = settlementRepository.findByTaskId(id);
                    if (!settlements.isEmpty())
                        settlementRepository.deleteAll(settlements);
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

            if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
                List<Skill> skills = skillRepository.findAllById(request.getRequiredSkills());
                if (skills.size() != request.getRequiredSkills().size())
                    throw new IllegalArgumentException("One or more skills not found.");
                existing.setRequiredSkills(new java.util.HashSet<>(skills));
            } else
                existing.setRequiredSkills(new java.util.HashSet<>());

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

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksByWorkerId(Long workerId) {
        return settlementRepository.findByWorkerId(workerId).stream()
                .map(s -> taskMapper.toDto(s.getTask()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Task> getOpenTasksForScheduling() { return taskRepository.findByStatusName(TaskStatusLevel.OPEN.name()); }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksByStatusId(Long statusId) {
        return taskRepository.findByStatusId(statusId).stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getValidPrerequisites(Long successorId) {
        // Build the graph ONCE outside the loop to prevent N+1 queries
        List<TaskConstraint> allConstraints = taskConstraintRepository.findAll();
        Map<Long, List<Long>> graph = cycleDetectionUtil.buildGraph(allConstraints);

        return getAllTasks().stream()
                .filter(possiblePred -> !possiblePred.getId().equals(successorId))
                .filter(possiblePred -> !cycleDetectionUtil.wouldCreateCycle(possiblePred.getId(), successorId, graph))
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
        if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
            List<Skill> skills = skillRepository.findAllById(request.getRequiredSkills());
            if (skills.size() != request.getRequiredSkills().size())
                throw new IllegalArgumentException("One or more skills not found.");
            builder.requiredSkills(new java.util.HashSet<>(skills));
        } else
            builder.requiredSkills(new java.util.HashSet<>());

        return builder.build();
    }
}
