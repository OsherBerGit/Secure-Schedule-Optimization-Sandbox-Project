package com.example.algorithm.controller;

import com.example.algorithm.db.ScheduleData;
import com.example.algorithm.dto.*;
import com.example.algorithm.engine.*;
import com.example.algorithm.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * REST Controller for the scheduling algorithm.
 *
 * Endpoint : POST /api/v1/algo/schedule
 * Consumes : application/json  (ScheduleRequest)
 * Produces : application/json  (ScheduleResponse)
 *
 * Completely stateless — no DB, no session, no security layer.
 * Main-backend sends a full data snapshot; this service returns assignments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/algo")
public class ScheduleController {

    // -------------------------------------------------------------------------
    // Endpoint
    // -------------------------------------------------------------------------

    /**
     * Runs the requested scheduling strategy over the provided users and tasks.
     *
     * @param request full snapshot of employees and tasks from main-backend
     * @return optimized task assignments with scheduled start/end times
     */
    @PostMapping("/schedule")
    public ResponseEntity<ScheduleResponse> schedule(@RequestBody ScheduleRequest request) {
        log.info("Received schedule request — strategy: {}, users: {}, tasks: {}",
                request.getStrategy(),
                request.getUsers() != null ? request.getUsers().size() : 0,
                request.getTasks() != null ? request.getTasks().size() : 0);

        // 1. Map DTOs → internal models
        ScheduleData data = mapToScheduleData(request);

        // 2. Resolve strategy (default: GREEDY)
        SchedulingStrategy strategy = resolveStrategy(request.getStrategy());
        Scheduler scheduler = new Scheduler(strategy);

        // 3. Run algorithm
        List<TaskAssignment> assignments = scheduler.run(data);

        // 4. Map results → response DTOs
        ScheduleResponse response = buildResponse(strategy.getName(), assignments);

        log.info("Schedule complete — assigned: {}, unassigned: {}",
                response.getAssignedTasks(), response.getUnassignedTasks());

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Mapping — Request DTOs → Internal Models
    // -------------------------------------------------------------------------

    private ScheduleData mapToScheduleData(ScheduleRequest request) {
        List<AlgoUser> users = mapUsers(request.getUsers());
        List<AlgoTask> tasks = mapTasks(request.getTasks());
        return new ScheduleData(users, tasks);
    }

    private List<AlgoUser> mapUsers(List<UserRequest> userRequests) {
        if (userRequests == null) return new ArrayList<>();
        List<AlgoUser> users = new ArrayList<>();
        for (UserRequest u : userRequests) {
            List<AlgoVacation> vacations = new ArrayList<>();
            if (u.getVacations() != null) {
                for (VacationRequest v : u.getVacations()) {
                    vacations.add(AlgoVacation.builder()
                            .id(v.getId())
                            .userId(u.getId())
                            .startDate(v.getStartDate())
                            .endDate(v.getEndDate())
                            .status("APPROVED")
                            .build());
                }
            }
            users.add(AlgoUser.builder()
                    .id(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .email(u.getEmail())
                    .dailyAvailabilityHours(u.getDailyAvailabilityHours())
                    .maxTasks(u.getMaxTasks())
                    .roles(u.getRoles() != null ? u.getRoles() : new HashSet<>())
                    .vacations(vacations)
                    .build());
        }
        return users;
    }

    private List<AlgoTask> mapTasks(List<TaskRequest> taskRequests) {
        if (taskRequests == null) return new ArrayList<>();
        List<AlgoTask> tasks = new ArrayList<>();
        for (TaskRequest t : taskRequests) {
            tasks.add(AlgoTask.builder()
                    .id(t.getId())
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .durationHours(t.getDurationHours())
                    .deadline(t.getDeadline())
                    .priority(t.getPriority())
                    .priorityLevel(t.getPriorityLevel())
                    .status(t.getStatus())
                    .requiredRoles(t.getRequiredRoles() != null ? t.getRequiredRoles() : new HashSet<>())
                    .predecessorTaskIds(t.getPredecessorTaskIds() != null ? t.getPredecessorTaskIds() : new ArrayList<>())
                    .successorTaskIds(t.getSuccessorTaskIds() != null ? t.getSuccessorTaskIds() : new ArrayList<>())
                    .build());
        }
        return tasks;
    }

    // -------------------------------------------------------------------------
    // Mapping — Internal Models → Response DTOs
    // -------------------------------------------------------------------------

    private ScheduleResponse buildResponse(String strategyName, List<TaskAssignment> assignments) {
        List<TaskAssignmentResponse> responseDtos = new ArrayList<>();
        int assigned = 0;

        for (TaskAssignment a : assignments) {
            boolean hasEmployee = a.getAssignedEmployee() != null;
            if (hasEmployee) assigned++;

            responseDtos.add(TaskAssignmentResponse.builder()
                    .taskId(a.getTask().getId())
                    .taskTitle(a.getTask().getTitle())
                    .assignedUserId(hasEmployee ? a.getAssignedEmployee().getId() : null)
                    .assignedUserFullName(hasEmployee
                            ? a.getAssignedEmployee().getFirstName() + " " + a.getAssignedEmployee().getLastName()
                            : null)
                    .scheduledStart(a.getScheduledStart())
                    .scheduledEnd(a.getScheduledEnd())
                    .reason(a.getReason())
                    .build());
        }

        return ScheduleResponse.builder()
                .strategyUsed(strategyName)
                .totalTasks(assignments.size())
                .assignedTasks(assigned)
                .unassignedTasks(assignments.size() - assigned)
                .assignments(responseDtos)
                .build();
    }

    // -------------------------------------------------------------------------
    // Strategy resolution
    // -------------------------------------------------------------------------

    private SchedulingStrategy resolveStrategy(String strategyName) {
        if ("ROUND_ROBIN".equalsIgnoreCase(strategyName)) {
            return new RoundRobinSchedulingStrategy();
        }
        return new GreedySchedulingStrategy(); // default
    }
}

