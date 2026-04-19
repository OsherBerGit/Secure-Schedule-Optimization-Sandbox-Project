package com.example.mainbackend.service;

import com.example.mainbackend.dto.taskpriority.TaskPriorityResponseDto;
import com.example.mainbackend.entity.TaskPriority;
import com.example.mainbackend.mapper.TaskPriorityMapper;
import com.example.mainbackend.repository.TaskPriorityRepository;
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
public class TaskPriorityService {

    private final TaskPriorityRepository taskPriorityRepository;
    private final TaskPriorityMapper mapper;

    @Transactional(readOnly = true)
    public TaskPriorityResponseDto getPriorityById(Long id) {
        TaskPriority priority = taskPriorityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Priority not found with ID: " + id));
        return mapper.toDto(priority);
    }

    @Transactional(readOnly = true)
    public List<TaskPriorityResponseDto> getAllPriorities() {
        return taskPriorityRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }
}
