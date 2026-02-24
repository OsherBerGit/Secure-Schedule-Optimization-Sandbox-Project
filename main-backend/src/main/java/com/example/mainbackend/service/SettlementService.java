package com.example.mainbackend.service;

import com.example.mainbackend.dto.settlement.SettlementCreateRequest;
import com.example.mainbackend.dto.settlement.SettlementResponseDto;
import com.example.mainbackend.entity.Settlement;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.mapper.SettlementMapper;
import com.example.mainbackend.repository.SettlementRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing settlements (final schedule assignments).
 * A settlement represents the assignment of a worker to a task with dates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SettlementMapper mapper;

    /**
     * Create a new settlement (assignment of worker to task).
     *
     * @param request the settlement creation request containing task ID and worker ID
     * @return the created settlement as a DTO
     * @throws IllegalArgumentException if task or worker not found
     */
    @Transactional
    public SettlementResponseDto createSettlement(SettlementCreateRequest request) {
        // Validate task exists
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + request.getTaskId()));

        // Validate worker exists
        User worker = userRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found with ID: " + request.getWorkerId()));

        // Build and save settlement
        Settlement settlement = Settlement.builder()
                .task(task)
                .worker(worker)
                .settlementDate(request.getSettlementDate())
                .completionDate(request.getCompletionDate())
                .build();

        Settlement saved = settlementRepository.save(settlement);
        return mapper.toDto(saved);
    }

    /**
     * Retrieve a settlement by its ID.
     *
     * @param id the settlement ID
     * @return the settlement as a DTO
     * @throws IllegalArgumentException if settlement not found
     */
    @Transactional(readOnly = true)
    public SettlementResponseDto getSettlementById(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found with ID: " + id));
        return mapper.toDto(settlement);
    }

    /**
     * Retrieve all settlements.
     *
     * @return list of all settlements as DTOs
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getAllSettlements() {
        return settlementRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Retrieve all settlements for a specific worker.
     *
     * @param workerId the worker ID
     * @return list of settlements for the worker
     * @throws IllegalArgumentException if worker not found
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getSettlementsByWorker(Long workerId) {
        // Validate worker exists
        if (!userRepository.existsById(workerId))
            throw new IllegalArgumentException("Worker not found with ID: " + workerId);

        return settlementRepository.findByWorkerId(workerId).stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Retrieve all settlements for a specific task.
     *
     * @param taskId the task ID
     * @return list of settlements for the task
     * @throws IllegalArgumentException if task not found
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getSettlementsByTask(Long taskId) {
        // Validate task exists
        if (!taskRepository.existsById(taskId))
            throw new IllegalArgumentException("Task not found with ID: " + taskId);

        return settlementRepository.findByTaskId(taskId).stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Retrieve settlements within a date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of settlements within the date range
     * @throws IllegalArgumentException if start date is after end date
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getSettlementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate))
            throw new IllegalArgumentException("Start date cannot be after end date");

        return settlementRepository.findBySettlementDateBetween(startDate, endDate).stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Delete a settlement by its ID.
     *
     * @param id the settlement ID to delete
     * @return true if deleted successfully
     * @throws IllegalArgumentException if settlement not found
     */
    @Transactional
    public boolean deleteSettlement(Long id) {
        if (!settlementRepository.existsById(id))
            throw new IllegalArgumentException("Settlement not found with ID: " + id);

        settlementRepository.deleteById(id);
        return true;
    }
}
