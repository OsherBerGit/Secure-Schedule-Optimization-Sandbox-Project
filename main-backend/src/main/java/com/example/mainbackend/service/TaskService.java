package com.example.mainbackend.service;

import com.example.mainbackend.constants.TaskStatusConstants;
import com.example.mainbackend.dto.task.TaskCreateRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.entity.Department;
import com.example.mainbackend.entity.Job;
import com.example.mainbackend.entity.Priority;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.mapper.TaskMapper;
import com.example.mainbackend.repository.*;
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
    private final PriorityRepository priorityRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final SettlementRepository settlementRepository;
    private final JobRepository jobRepository; // Added dependency
    private final TaskMapper taskMapper;
    private final SecurityHelper securityHelper;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public TaskResponseDto createTask(TaskCreateRequest request) {
        Task task = buildTaskFromRequest(request, null);

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
            Task updated = buildTaskFromRequest(request, existing);
            return taskMapper.toDto(taskRepository.save(updated));
        });
    }

    @Transactional
    public boolean deleteTask(Long id) {
        if (!taskRepository.existsById(id)) return false;
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
     * No category check needed — task_statuses table holds only task lifecycle statuses.
     */
    @Transactional(readOnly = true)
    public List<Task> getOpenTasksForScheduling() {
        return taskRepository.findByStatusName(TaskStatusConstants.TASK_OPEN);
    }

    private Task buildTaskFromRequest(TaskCreateRequest request, Task existing) {
        Priority priority = priorityRepository.findById(request.getPriorityId())
                .orElseThrow(() -> new RuntimeException("Priority not found: " + request.getPriorityId()));

        // Default status for new tasks is OPEN
        TaskStatus openStatus = taskStatusRepository.findByName(TaskStatusConstants.TASK_OPEN)
                .orElseThrow(() -> new IllegalStateException("OPEN status not seeded in task_statuses"));

        Task.TaskBuilder builder = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .durationHours(request.getDurationHours())
                .priority(priority)
                .status(existing != null ? existing.getStatus() : openStatus);

        // Handle required Job
        if (request.getRequiredJob() != null) {
            Job job = jobRepository.findById(request.getRequiredJob())
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + request.getRequiredJob()));
            builder.requiredJob(job);
        } else if (existing != null) {
            // Keep existing job if not updating it?
            // Usually update semantics differ (PATCH vs PUT), but here we assume full update or we check null.
            // Request uses `requiredJobId`, DTO validation ensures it is NotNull for create.
            // For update, if null, we might keep existing. But TaskCreateRequest has @NotNull on it if checked.
            builder.requiredJob(existing.getRequiredJob());
        }

        if (existing != null) {
            builder.id(existing.getId())
                    .startTime(existing.getStartTime())
                    .outgoingConstraints(existing.getOutgoingConstraints())
                    .incomingConstraints(existing.getIncomingConstraints());
        }

        return builder.build();
    }
}
