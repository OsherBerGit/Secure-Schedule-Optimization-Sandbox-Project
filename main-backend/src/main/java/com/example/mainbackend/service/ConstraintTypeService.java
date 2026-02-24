package com.example.mainbackend.service;

import com.example.mainbackend.dto.constrainttype.ConstraintTypeRequest;
import com.example.mainbackend.dto.constrainttype.ConstraintTypeResponseDto;
import com.example.mainbackend.entity.ConstraintType;
import com.example.mainbackend.mapper.ConstraintTypeMapper;
import com.example.mainbackend.repository.ConstraintTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing task constraint types (lookup table).
 * Constraint types define relationships between tasks (e.g., FINISH_TO_START, START_TO_START).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConstraintTypeService {

    private final ConstraintTypeRepository constraintTypeRepository;
    private final ConstraintTypeMapper mapper;

    /**
     * Create a new task constraint type (e.g., FINISH_TO_START).
     *
     * @param request the constraint type creation request
     * @return the created constraint type as a DTO
     * @throws IllegalArgumentException if a constraint type with the same name already exists
     */
    @Transactional
    public ConstraintTypeResponseDto createConstraintType(ConstraintTypeRequest request) {
        // Check if constraint type with this name already exists
        if (constraintTypeRepository.findByName(request.getName()).isPresent())
            throw new IllegalArgumentException("Constraint type with name '" + request.getName() + "' already exists");

        ConstraintType constraintType = ConstraintType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        ConstraintType saved = constraintTypeRepository.save(constraintType);
        return mapper.toDto(saved);
    }

    /**
     * Retrieve a constraint type by its ID.
     *
     * @param id the constraint type ID
     * @return the constraint type as a DTO
     * @throws IllegalArgumentException if constraint type not found
     */
    @Transactional(readOnly = true)
    public ConstraintTypeResponseDto getConstraintTypeById(Long id) {
        ConstraintType constraintType = constraintTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint type not found with ID: " + id));
        return mapper.toDto(constraintType);
    }

    /**
     * Retrieve all constraint types.
     *
     * @return list of all constraint types as DTOs
     */
    @Transactional(readOnly = true)
    public List<ConstraintTypeResponseDto> getAllConstraintTypes() {
        return constraintTypeRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Update an existing constraint type.
     *
     * @param id the constraint type ID to update
     * @param request the updated constraint type data
     * @return the updated constraint type as a DTO
     * @throws IllegalArgumentException if constraint type not found or name already exists
     */
    @Transactional
    public ConstraintTypeResponseDto updateConstraintType(Long id, ConstraintTypeRequest request) {
        ConstraintType existing = constraintTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint type not found with ID: " + id));

        // Check if another constraint type with this name already exists
        constraintTypeRepository.findByName(request.getName()).ifPresent(constraintType -> {
            if (!constraintType.getId().equals(id))
                throw new IllegalArgumentException("Constraint type with name '" + request.getName() + "' already exists");
        });

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        ConstraintType updated = constraintTypeRepository.save(existing);

        return mapper.toDto(updated);
    }

    /**
     * Delete a constraint type by its ID.
     * Note: In production, check if any task constraints are using this type before deletion.
     *
     * @param id the constraint type ID to delete
     * @return true if deleted successfully
     * @throws IllegalArgumentException if constraint type not found
     */
    @Transactional
    public boolean deleteConstraintType(Long id) {
        if (!constraintTypeRepository.existsById(id))
            throw new IllegalArgumentException("Constraint type not found with ID: " + id);

        // Note: In production, you might want to check if any task constraints are using this type
        // and prevent deletion or handle it appropriately

        constraintTypeRepository.deleteById(id);
        return true;
    }
}
