package com.example.mainbackend.service;

import com.example.mainbackend.dto.priority.PriorityRequest;
import com.example.mainbackend.dto.priority.PriorityResponseDto;
import com.example.mainbackend.entity.Priority;
import com.example.mainbackend.mapper.PriorityMapper;
import com.example.mainbackend.repository.PriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing task priorities (lookup table).
 * Priorities define the importance level of tasks (e.g., LOW, MEDIUM, HIGH, CRITICAL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriorityService {

    private final PriorityRepository priorityRepository;
    private final PriorityMapper mapper;

    /**
     * Create a new task priority level.
     *
     * @param request the priority creation request
     * @return the created priority as a DTO
     * @throws IllegalArgumentException if a priority with the same name already exists
     */
    @Transactional
    public PriorityResponseDto createPriority(PriorityRequest request) {
        // Check if priority with this name already exists
        if (priorityRepository.findByName(request.getName()).isPresent())
            throw new IllegalArgumentException("Priority with name '" + request.getName() + "' already exists");

        Priority priority = Priority.builder()
                .name(request.getName())
                .build();

        Priority saved = priorityRepository.save(priority);
        return mapper.toDto(saved);
    }

    /**
     * Retrieve a priority by its ID.
     *
     * @param id the priority ID
     * @return the priority as a DTO
     * @throws IllegalArgumentException if priority not found
     */
    @Transactional(readOnly = true)
    public PriorityResponseDto getPriorityById(Long id) {
        Priority priority = priorityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Priority not found with ID: " + id));
        return mapper.toDto(priority);
    }

    /**
     * Retrieve all priorities.
     *
     * @return list of all priorities as DTOs
     */
    @Transactional(readOnly = true)
    public List<PriorityResponseDto> getAllPriorities() {
        return priorityRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Update an existing priority.
     *
     * @param id the priority ID to update
     * @param request the updated priority data
     * @return the updated priority as a DTO
     * @throws IllegalArgumentException if priority not found or name already exists
     */
    @Transactional
    public PriorityResponseDto updatePriority(Long id, PriorityRequest request) {
        Priority existing = priorityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Priority not found with ID: " + id));

        // Check if another priority with this name already exists
        priorityRepository.findByName(request.getName()).ifPresent(priority -> {
            if (!priority.getId().equals(id))
                throw new IllegalArgumentException("Priority with name '" + request.getName() + "' already exists");
        });

        existing.setName(request.getName());
        Priority updated = priorityRepository.save(existing);

        return mapper.toDto(updated);
    }

    /**
     * Delete a priority by its ID.
     * Note: In production, check if any tasks are using this priority before deletion.
     *
     * @param id the priority ID to delete
     * @return true if deleted successfully
     * @throws IllegalArgumentException if priority not found
     */
    @Transactional
    public boolean deletePriority(Long id) {
        if (!priorityRepository.existsById(id))
            throw new IllegalArgumentException("Priority not found with ID: " + id);

        // Note: In production, you might want to check if any tasks are using this priority
        // and prevent deletion or handle it appropriately

        priorityRepository.deleteById(id);
        return true;
    }
}
