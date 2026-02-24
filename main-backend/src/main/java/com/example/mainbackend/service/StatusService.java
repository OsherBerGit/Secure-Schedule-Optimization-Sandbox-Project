package com.example.mainbackend.service;

import com.example.mainbackend.dto.status.StatusRequest;
import com.example.mainbackend.dto.status.StatusResponseDto;
import com.example.mainbackend.entity.Status;
import com.example.mainbackend.mapper.StatusMapper;
import com.example.mainbackend.repository.StatusRepository;
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
public class StatusService {

    private final StatusRepository statusRepository;
    private final StatusMapper mapper;

    /**
     * Create a new task status.
     *
     * @param request the status creation request
     * @return the created status as a DTO
     * @throws IllegalArgumentException if a status with the same name already exists
     */
    @Transactional
    public StatusResponseDto createStatus(StatusRequest request) {
        // Check if status with this name already exists
        if (statusRepository.findByName(request.getName()).isPresent())
            throw new IllegalArgumentException("Status with name '" + request.getName() + "' already exists");

        Status status = Status.builder()
                .name(request.getName())
                .build();

        Status saved = statusRepository.save(status);
        return mapper.toDto(saved);
    }

    /**
     * Retrieve a status by its ID.
     *
     * @param id the status ID
     * @return the status as a DTO
     * @throws IllegalArgumentException if status not found
     */
    @Transactional(readOnly = true)
    public StatusResponseDto getStatusById(Long id) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found with ID: " + id));
        return mapper.toDto(status);
    }

    /**
     * Retrieve all statuses.
     *
     * @return list of all statuses as DTOs
     */
    @Transactional(readOnly = true)
    public List<StatusResponseDto> getAllStatuses() {
        return statusRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Update an existing status.
     *
     * @param id the status ID to update
     * @param request the updated status data
     * @return the updated status as a DTO
     * @throws IllegalArgumentException if status not found or name already exists
     */
    @Transactional
    public StatusResponseDto updateStatus(Long id, StatusRequest request) {
        Status existing = statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found with ID: " + id));

        // Check if another status with this name already exists
        statusRepository.findByName(request.getName()).ifPresent(status -> {
            if (!status.getId().equals(id))
                throw new IllegalArgumentException("Status with name '" + request.getName() + "' already exists");
        });

        existing.setName(request.getName());
        Status updated = statusRepository.save(existing);

        return mapper.toDto(updated);
    }

    /**
     * Delete a status by its ID.
     * Note: In production, check if any tasks are using this status before deletion.
     *
     * @param id the status ID to delete
     * @return true if deleted successfully
     * @throws IllegalArgumentException if status not found
     */
    @Transactional
    public boolean deleteStatus(Long id) {
        if (!statusRepository.existsById(id))
            throw new IllegalArgumentException("Status not found with ID: " + id);

        // Note: In production, you might want to check if any tasks are using this status
        // and prevent deletion or handle it appropriately

        statusRepository.deleteById(id);
        return true;
    }
}
