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

    @Transactional(readOnly = true)
    public ConstraintTypeResponseDto getConstraintTypeById(Long id) {
        ConstraintType constraintType = constraintTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constraint type not found with ID: " + id));
        return mapper.toDto(constraintType);
    }

    @Transactional(readOnly = true)
    public List<ConstraintTypeResponseDto> getAllConstraintTypes() {
        return constraintTypeRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }
}
