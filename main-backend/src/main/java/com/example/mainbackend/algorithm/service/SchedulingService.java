package com.example.mainbackend.algorithm.service;

import com.example.mainbackend.algorithm.AlgorithmClient;
import com.example.mainbackend.algorithm.dto.AlgoScheduleRequest;
import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import com.example.mainbackend.algorithm.dto.AlgoTaskAssignmentResponse;
import com.example.mainbackend.algorithm.dto.AlgoTaskRequest;
import com.example.mainbackend.algorithm.dto.AlgoUserRequest;
import com.example.mainbackend.algorithm.dto.AlgoVacationRequest;
import com.example.mainbackend.constants.TaskStatusConstants;
import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.Settlement;
import com.example.mainbackend.entity.SettlementStatus;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskStatus;
import com.example.mainbackend.repository.SettlementRepository;
import com.example.mainbackend.repository.SettlementStatusRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.repository.TaskStatusRepository;
import com.example.mainbackend.repository.UserRepository;
import com.example.mainbackend.repository.VacationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the full scheduling flow:
 *  1. Fetch only OPEN tasks (lifecycle filter — Zero-Trust: LOCKED/CLOSED excluded)
 *  2. Build a minimal anonymous request for the algorithm service
 *  3. Apply results: Task → LOCKED, Settlement → PENDING (separate tables/entities)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final VacationRepository vacationRepository;
    private final SettlementRepository settlementRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final SettlementStatusRepository settlementStatusRepository;
    private final AlgorithmClient algorithmClient;

    @Transactional
    public AlgoScheduleResponse runScheduling(String strategy) {
        AlgoScheduleRequest request = buildRequest(strategy);
        AlgoScheduleResponse response = algorithmClient.requestSchedule(request);
        applyResults(response);
        return response;
    }

    // ── Build request ─────────────────────────────────────────────────────────

    private AlgoScheduleRequest buildRequest(String strategy) {
        return AlgoScheduleRequest.builder()
                .strategy(strategy)
                .users(buildUserRequests())
                .tasks(buildTaskRequests())
                .build();
    }

    private List<AlgoUserRequest> buildUserRequests() {
        return userRepository.findAll().stream().map(user -> {
            List<AlgoVacationRequest> vacations = vacationRepository
                    .findByWorkerId(user.getId()).stream()
                    .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus().getName()))
                    .map(v -> AlgoVacationRequest.builder()
                            .id(v.getId())
                            .startDate(v.getStartDate())
                            .endDate(v.getEndDate())
                            .build())
                    .collect(Collectors.toList());

            return AlgoUserRequest.builder()
                    .id(user.getId())
                    .dailyAvailabilityHours(user.getDailyAvailabilityHours())
                    .maxTasks(user.getMaxTasks())
                    .roles(user.getRoles().stream()
                            .map(Role::getId)
                            .collect(Collectors.toSet()))
                    .vacations(vacations)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Zero-Trust filter: only OPEN tasks are sent to the algorithm.
     * task_statuses table holds only Task lifecycle values — no category check needed.
     */
    private List<AlgoTaskRequest> buildTaskRequests() {
        List<Task> openTasks = taskRepository.findByStatusName(TaskStatusConstants.TASK_OPEN);
        log.info("Sending {} OPEN tasks to algorithm (LOCKED/CLOSED excluded)", openTasks.size());

        return openTasks.stream().map(task -> {
            List<Long> predecessorIds = task.getIncomingConstraints().stream()
                    .map(tc -> tc.getPredecessorTask().getId())
                    .collect(Collectors.toList());

            return AlgoTaskRequest.builder()
                    .id(task.getId())
                    .durationHours(task.getDurationHours())
                    .deadline(task.getDeadline())
                    .priorityLevel(task.getPriority().getValue())
                    .requiredRoles(task.getRequiredRoles().stream()
                            .map(Role::getId)
                            .collect(Collectors.toSet()))
                    .predecessorTaskIds(predecessorIds)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Apply results ─────────────────────────────────────────────────────────

    private void applyResults(AlgoScheduleResponse response) {
        if (response == null || response.getAssignments() == null) return;

        TaskStatus lockedStatus = taskStatusRepository.findByName(TaskStatusConstants.TASK_LOCKED)
                .orElseThrow(() -> new IllegalStateException("LOCKED status not seeded in task_statuses"));

        SettlementStatus pendingStatus = settlementStatusRepository.findByName(TaskStatusConstants.SETTLEMENT_PENDING)
                .orElseThrow(() -> new IllegalStateException("PENDING status not seeded in settlement_statuses"));

        for (AlgoTaskAssignmentResponse assignment : response.getAssignments()) {
            if (assignment.getAssignedUserId() == null) continue;

            taskRepository.findById(assignment.getTaskId()).ifPresent(task ->
                userRepository.findById(assignment.getAssignedUserId()).ifPresent(user -> {

                    // 1. Task lifecycle → LOCKED
                    task.setStartTime(assignment.getScheduledStart());
                    task.setStatus(lockedStatus);
                    taskRepository.save(task);

                    // 2. Create Settlement → PENDING (only if not already settled)
                    boolean alreadySettled = settlementRepository.findByTaskId(task.getId()).stream()
                            .anyMatch(s -> s.getWorker().getId().equals(user.getId()));

                    if (!alreadySettled) {
                        settlementRepository.save(Settlement.builder()
                                .task(task)
                                .worker(user)
                                .status(pendingStatus)
                                .settlementDate(LocalDateTime.now())
                                .completionDate(null)
                                .build());
                        log.debug("Task [{}] → LOCKED; Settlement → PENDING for user [{}]",
                                task.getId(), user.getId());
                    }
                })
            );
        }
        log.info("Applied {} assignments: tasks LOCKED, settlements PENDING", response.getAssignedTasks());
    }
}
