package com.example.mainbackend.service;

import com.example.mainbackend.constants.SettlementStatusLevel;
import com.example.mainbackend.constants.TaskStatusLevel;
import com.example.mainbackend.dto.settlement.SettlementCreateRequest;
import com.example.mainbackend.dto.settlement.SettlementResponseDto;
import com.example.mainbackend.entity.Settlement;
import com.example.mainbackend.entity.SettlementStatus;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.mapper.SettlementMapper;
import com.example.mainbackend.repository.SettlementRepository;
import com.example.mainbackend.repository.SettlementStatusRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.repository.TaskStatusRepository;
import com.example.mainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final SettlementStatusRepository settlementStatusRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final SettlementMapper mapper;

    /**
     * Create a new settlement (assignment of worker to task).
     * Status defaults to ASSIGNED if not specified in request.
     */
    @Transactional
    public SettlementResponseDto createSettlement(SettlementCreateRequest request) {
        // Validate task exists
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + request.getTaskId()));

        // Validate worker exists
        User worker = userRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found with ID: " + request.getWorkerId()));

        // Resolve settlement status — default to PENDING
        SettlementStatus status;
        if (request.getStatusId() != null) {
            status = settlementStatusRepository.findById(request.getStatusId())
                    .orElseThrow(() -> new IllegalArgumentException("Settlement status not found: " + request.getStatusId()));
        } else {
            status = settlementStatusRepository.findByName(SettlementStatusLevel.PENDING.name())
                    .orElseThrow(() -> new IllegalStateException("PENDING status not seeded in settlement_statuses"));
        }

        // Build and save settlement
        Settlement settlement = Settlement.builder()
                .task(task)
                .worker(worker)
                .status(status)
                .settlementDate(request.getSettlementDate())
                .completionDate(request.getCompletionDate())
                .build();

        Settlement saved = settlementRepository.save(settlement);
        return mapper.toDto(saved);
    }

    /**
     * Retrieve a settlement by its ID.
     */
    @Transactional(readOnly = true)
    public SettlementResponseDto getSettlementById(Long id) {
        Settlement settlement = settlementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found with ID: " + id));
        return mapper.toDto(settlement);
    }

    /**
     * Retrieve all settlements (eagerly loaded to prevent LazyInitializationException).
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getAllSettlements() {
        return settlementRepository.findAllWithDetails().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Retrieve all settlements for a specific worker.
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getSettlementsByWorker(Long workerId) {
        if (!userRepository.existsById(workerId))
            throw new IllegalArgumentException("Worker not found with ID: " + workerId);

        return settlementRepository.findByWorkerId(workerId).stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Returns all settlements for the currently authenticated worker (by nationalId from JWT).
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getMySettlements(String nationalId) {
        return settlementRepository.findByWorker_NationalId(nationalId).stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Marks a settlement as COMPLETED.
     * <ul>
     *   <li>WORKER — must own the settlement (no IDOR traversal).</li>
     *   <li>ADMIN  — may complete any settlement (management override).</li>
     * </ul>
     */
    @Transactional
    public SettlementResponseDto completeSettlement(Long id, String nationalId) {
        Settlement settlement = settlementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found with ID: " + id));

        // Determine if the caller is an ADMIN — skip ownership check if so
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !settlement.getWorker().getNationalId().equals(nationalId)) {
            throw new SecurityException("Access denied: this settlement does not belong to you");
        }

        SettlementStatus completed = settlementStatusRepository.findByName(SettlementStatusLevel.COMPLETED.name())
                .orElseThrow(() -> new IllegalStateException("COMPLETED status not seeded in settlement_statuses"));

        settlement.setStatus(completed);
        settlement.setCompletionDate(LocalDateTime.now());
        settlementRepository.save(settlement);

        log.info("Settlement [{}] marked COMPLETED by '{}' (admin={})", id, nationalId, isAdmin);

        // If every settlement for this task is now COMPLETED, close the task lifecycle
        Task task = settlement.getTask();
        List<Settlement> allForTask = settlementRepository.findByTaskId(task.getId());
        boolean allDone = allForTask.stream()
                .allMatch(s -> SettlementStatusLevel.COMPLETED.name().equals(s.getStatus().getName()));
        if (allDone) {
            TaskStatus closed = taskStatusRepository.findByName(TaskStatusLevel.CLOSED.name())
                    .orElseThrow(() -> new IllegalStateException("CLOSED status not seeded in task_statuses"));
            task.setStatus(closed);
            taskRepository.save(task);
            log.info("Task [{}] '{}' transitioned to CLOSED — all settlements completed", task.getId(), task.getTitle());
        }

        return mapper.toDto(settlement);
    }

    /**
     * Retrieve all settlements for a specific task.
     */
    @Transactional(readOnly = true)
    public List<SettlementResponseDto> getSettlementsByTask(Long taskId) {
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
