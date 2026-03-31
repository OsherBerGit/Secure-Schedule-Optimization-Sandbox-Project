package com.example.mainbackend.service;

import com.example.mainbackend.dto.taskstatus.TaskStatusRequest;
import com.example.mainbackend.dto.taskstatus.TaskStatusResponseDto;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.mapper.TaskStatusMapper;
import com.example.mainbackend.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing task statuses (lookup table).
 * Statuses define the current state of a task (e.g., PENDING, IN_PROGRESS, COMPLETED).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskStatusService {

    private final TaskStatusRepository statusRepository;
    private final TaskStatusMapper mapper;

    /**
     * Create a new task status.
     *
     * @param request the status creation request
     * @return the created status as a DTO
     * @throws IllegalArgumentException if a status with the same name already exists
     */
    @Transactional
    public TaskStatusResponseDto createStatus(TaskStatusRequest request) {
        // Check if status with this name already exists
        if (statusRepository.findByName(request.getName()).isPresent())
            throw new IllegalArgumentException("Status with name '" + request.getName() + "' already exists");

        TaskStatus status = TaskStatus.builder()
                .name(request.getName())
                .build();

        TaskStatus saved = statusRepository.save(status);
        return mapper.toDto(saved);
    }

    /**
     * Retrieve a status by its ID.
     */
    @Transactional(readOnly = true)
    public TaskStatusResponseDto getStatusById(Long id) {
        TaskStatus status = statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TaskStatus not found with ID: " + id));
        return mapper.toDto(status);
    }

    /**
     * Retrieve all statuses.
     */
    @Transactional(readOnly = true)
    public List<TaskStatusResponseDto> getAllStatuses() {
        return statusRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Update an existing status.
     */
    @Transactional
    public TaskStatusResponseDto updateStatus(Long id, TaskStatusRequest request) {
        TaskStatus existing = statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TaskStatus not found with ID: " + id));

        statusRepository.findByName(request.getName()).ifPresent(status -> {
            if (!status.getId().equals(id))
                throw new IllegalArgumentException("TaskStatus with name '" + request.getName() + "' already exists");
        });

        existing.setName(request.getName());
        TaskStatus updated = statusRepository.save(existing);

        return mapper.toDto(updated);
    }

    /**
     * Delete a status by its ID.
     */
    @Transactional
    public boolean deleteStatus(Long id) {
        if (!statusRepository.existsById(id))
            throw new IllegalArgumentException("TaskStatus not found with ID: " + id);

        // Note: In production, you might want to check if any tasks are using this status
        // and prevent deletion or handle it appropriately

        statusRepository.deleteById(id);
        return true;
    }
}
