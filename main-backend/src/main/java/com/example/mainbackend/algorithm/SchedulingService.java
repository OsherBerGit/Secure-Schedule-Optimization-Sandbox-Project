package com.example.mainbackend.algorithm;

import com.example.mainbackend.algorithm.dto.AlgoScheduleRequest;
import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import com.example.mainbackend.algorithm.dto.AlgoTaskAssignmentResponse;
import com.example.mainbackend.algorithm.dto.AlgoTaskRequest;
import com.example.mainbackend.algorithm.dto.AlgoUserRequest;
import com.example.mainbackend.algorithm.dto.AlgoVacationRequest;
import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.Settlement;
import com.example.mainbackend.repository.SettlementRepository;
import com.example.mainbackend.repository.TaskRepository;
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
 *  1. Fetch all users + approved vacations + tasks from DB
 *  2. Build and send an anonymous request to the algorithm service (Zero-Trust)
 *  3. Apply returned assignments back to DB exclusively via Settlement records
 *     — Task.assignedEmployee has been removed; Settlement is the single source of truth
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final VacationRepository vacationRepository;
    private final SettlementRepository settlementRepository;
    private final AlgorithmClient algorithmClient;

    /**
     * Runs the scheduling algorithm and persists the results.
     *
     * @param strategy "GREEDY" or "ROUND_ROBIN"
     * @return the full response from the algorithm service
     */
    @Transactional
    public AlgoScheduleResponse runScheduling(String strategy) {
        // 1. Build the request from current DB state
        AlgoScheduleRequest request = buildRequest(strategy);

        // 2. Call algorithm service
        AlgoScheduleResponse response = algorithmClient.requestSchedule(request);

        // 3. Apply results back to DB
        applyResults(response);

        return response;
    }

    // -------------------------------------------------------------------------
    // Build request
    // -------------------------------------------------------------------------

    private AlgoScheduleRequest buildRequest(String strategy) {
        List<AlgoUserRequest> users = buildUserRequests();
        List<AlgoTaskRequest> tasks = buildTaskRequests();
        return AlgoScheduleRequest.builder()
                .strategy(strategy)
                .users(users)
                .tasks(tasks)
                .build();
    }

    private List<AlgoUserRequest> buildUserRequests() {
        return userRepository.findAll().stream().map(user -> {
            // Only include APPROVED vacations
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
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .dailyAvailabilityHours(user.getDailyAvailabilityHours())
                    .maxTasks(user.getMaxTasks())
                    .roles(user.getRoles().stream()
                            .map(Role::getRoleName)
                            .collect(Collectors.toSet()))
                    .vacations(vacations)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<AlgoTaskRequest> buildTaskRequests() {
        return taskRepository.findAll().stream().map(task -> {
            List<Long> predecessorIds = task.getIncomingConstraints().stream()
                    .map(tc -> tc.getPredecessorTask().getId())
                    .collect(Collectors.toList());

            List<Long> successorIds = task.getOutgoingConstraints().stream()
                    .map(tc -> tc.getSuccessorTask().getId())
                    .collect(Collectors.toList());

            return AlgoTaskRequest.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .durationHours(task.getDurationHours())
                    .deadline(task.getDeadline())
                    .priority(task.getPriority().getName())
                    .priorityLevel(task.getPriority().getValue())
                    .status(task.getStatus().getName())
                    .requiredRoles(task.getRequiredRoles().stream()
                            .map(Role::getRoleName)
                            .collect(Collectors.toSet()))
                    .predecessorTaskIds(predecessorIds)
                    .successorTaskIds(successorIds)
                    .build();
        }).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Apply results back to DB via Settlement (single source of truth)
    // -------------------------------------------------------------------------

    private void applyResults(AlgoScheduleResponse response) {
        if (response == null || response.getAssignments() == null) return;

        for (AlgoTaskAssignmentResponse assignment : response.getAssignments()) {
            if (assignment.getAssignedUserId() == null) continue; // skip unassigned

            taskRepository.findById(assignment.getTaskId()).ifPresent(task -> {
                userRepository.findById(assignment.getAssignedUserId()).ifPresent(user -> {

                    // Update startTime on the Task (scheduling metadata — stays on Task)
                    task.setStartTime(assignment.getScheduledStart());
                    taskRepository.save(task);

                    // Create or update a Settlement record as the single source of truth
                    // for the worker→task assignment produced by the algorithm
                    boolean alreadySettled = settlementRepository
                            .findByTaskId(task.getId()).stream()
                            .anyMatch(s -> s.getWorker().getId().equals(user.getId()));

                    if (!alreadySettled) {
                        Settlement settlement = Settlement.builder()
                                .task(task)
                                .worker(user)
                                .settlementDate(LocalDateTime.now())
                                .completionDate(null) // not yet completed
                                .build();
                        settlementRepository.save(settlement);
                        log.debug("Created settlement: task [{}] '{}' -> user [{}] '{} {}'",
                                task.getId(), task.getTitle(),
                                user.getId(), user.getFirstName(), user.getLastName());
                    } else {
                        log.debug("Settlement already exists for task [{}] and user [{}] — skipping",
                                task.getId(), user.getId());
                    }
                });
            });
        }

        log.info("Applied {} task assignments as Settlements", response.getAssignedTasks());
    }
}
