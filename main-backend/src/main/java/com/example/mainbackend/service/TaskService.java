package com.example.mainbackend.service;

import com.example.mainbackend.dto.task.TaskCreateRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.entity.Priority;
import com.example.mainbackend.entity.Status;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.mapper.TaskMapper;
import com.example.mainbackend.repository.PriorityRepository;
import com.example.mainbackend.repository.StatusRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.repository.UserRepository;
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
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponseDto createTask(TaskCreateRequest request) {
        Task task = buildTaskFromRequest(request, null);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toDto(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<TaskResponseDto> getTaskById(Long id) {
        return taskRepository.findById(id)
                .map(taskMapper::toDto);
    }

    @Transactional
    public Optional<TaskResponseDto> updateTask(Long id, TaskCreateRequest request) {
        return taskRepository.findById(id)
                .map(existingTask -> {
                    Task updatedTask = buildTaskFromRequest(request, existingTask);
                    Task savedTask = taskRepository.save(updatedTask);
                    return taskMapper.toDto(savedTask);
                });
    }

    @Transactional
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksByWorkerId(Long workerId) {
        return taskRepository.findByAssignedEmployeeId(workerId).stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksByStatus(String statusName) {
        return taskRepository.findByStatusName(statusName).stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to build a Task entity from TaskCreateRequest.
     * Fetches related entities (Priority, Status, User) from database and validates they exist.
     * Preserves existing relationships and fields when updating.
     *
     * @param request the task creation/update request
     * @param existingTask the existing task for updates (null for new tasks)
     * @return Task entity ready to be saved
     * @throws RuntimeException if any required related entity is not found
     */
    private Task buildTaskFromRequest(TaskCreateRequest request, Task existingTask) {
        // Fetch Priority (required)
        Priority priority = priorityRepository.findById(request.getPriorityId())
                .orElseThrow(() -> new RuntimeException("Priority not found with id: " + request.getPriorityId()));

        // Fetch Status (required)
        Status status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new RuntimeException("Status not found with id: " + request.getStatusId()));

        // Fetch User (optional - can be null)
        User assignedWorker = null;
        if (request.getAssignedWorkerId() != null) {
            assignedWorker = userRepository.findById(request.getAssignedWorkerId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getAssignedWorkerId()));
        }

        // Build task - use existing task for updates to preserve ID and other fields
        Task.TaskBuilder builder = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .durationHours(request.getDurationHours())
                .priority(priority)
                .status(status)
                .assignedEmployee(assignedWorker);

        // Preserve ID and other fields for updates
        if (existingTask != null) {
            builder.id(existingTask.getId())
                    .startTime(existingTask.getStartTime())
                    .requiredRoles(existingTask.getRequiredRoles())
                    .outgoingConstraints(existingTask.getOutgoingConstraints())
                    .incomingConstraints(existingTask.getIncomingConstraints());
        }

        return builder.build();
    }
}
