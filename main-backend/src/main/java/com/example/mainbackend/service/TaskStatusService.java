package com.example.mainbackend.service;

import com.example.mainbackend.dto.taskstatus.TaskStatusResponseDto;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.mapper.TaskStatusMapper;
import com.example.mainbackend.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskStatusService {

    private final TaskStatusRepository statusRepository;
    private final TaskStatusMapper mapper;

    @Transactional(readOnly = true)
    public TaskStatusResponseDto getStatusById(Long id) {
        TaskStatus status = statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TaskStatus not found with ID: " + id));
        return mapper.toDto(status);
    }

    @Transactional(readOnly = true)
    public List<TaskStatusResponseDto> getAllStatuses() {
        return statusRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }
}
