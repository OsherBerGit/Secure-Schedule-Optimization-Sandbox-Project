package com.example.mainbackend.service;

import com.example.mainbackend.constants.TaskStatusConstants;
import com.example.mainbackend.dto.task.TaskCreateRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.entity.Priority;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.mapper.TaskMapper;
import com.example.mainbackend.repository.PriorityRepository;
import com.example.mainbackend.repository.SettlementRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
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
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponseDto createTask(TaskCreateRequest request) {
        return taskMapper.toDto(taskRepository.save(buildTaskFromRequest(request, null)));
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getAllTasks() {
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
                .status(openStatus);

        if (existing != null)
            builder.id(existing.getId())
                    .startTime(existing.getStartTime())
                    .status(existing.getStatus())            // preserve lifecycle on update
                    .requiredRoles(existing.getRequiredRoles())
                    .outgoingConstraints(existing.getOutgoingConstraints())
                    .incomingConstraints(existing.getIncomingConstraints());

        return builder.build();
    }
}
