package com.example.sidebackend.service;

import com.example.algorithm.engine.*;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.TaskAssignment;
import com.example.sidebackend.dto.*;
import com.example.sidebackend.dto.SchedulingResponseDto.AssignmentDto;
import com.example.sidebackend.service.AlgoMapper.MappedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AlgoService {

    private static final Logger log = LoggerFactory.getLogger(AlgoService.class);
    private static final String DEFAULT_STRATEGY = "GREEDY";

    private final AlgoMapper algoMapper;
    private final TopologicalSorter topologicalSorter = new TopologicalSorter();

    public AlgoService(AlgoMapper algoMapper) { this.algoMapper = algoMapper; }

    public SchedulingResponseDto schedule(SchedulingRequestDto request) {
        sanitize(request);

        MappedRequest mapped = algoMapper.toModels(request);

        Comparator<AlgoTask> taskPriorityComparator = Comparator
                .comparingInt((AlgoTask t) -> t.getPriorityLevel() != null ? t.getPriorityLevel() : 0)
                .reversed()
                .thenComparing(AlgoTask::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));

        List<AlgoTask> sortedTasks;
        try {
            sortedTasks = topologicalSorter.sort(mapped.tasks(), taskPriorityComparator);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("DAG Validation failed: " + e.getMessage());
        }

        ScheduleData data = new ScheduleData(mapped.users(), sortedTasks);

        SchedulingStrategy strategy = resolveStrategy(request.strategy(), mapped.config());
        log.info("[AlgoService] Running strategy '{}' with {} user(s) and {} task(s)",
                strategy.getName(), mapped.users().size(), mapped.tasks().size());

        Scheduler scheduler = new Scheduler(strategy);
        List<TaskAssignment> assignments = scheduler.run(data);

        return buildResponse(strategy, mapped.tasks().size(), assignments);
    }

    // ─── Step 1: Sanitization ─────────────────────────────────────────────────

    private void sanitize(SchedulingRequestDto request) {
        List<Long> userIds = request.users().stream().map(UserDto::id).toList();
        if (userIds.stream().distinct().count() != userIds.size())
            throw new IllegalArgumentException("Duplicate user IDs detected in the request.");

        List<Long> taskIds = request.tasks().stream().map(TaskDto::id).toList();
        if (taskIds.stream().distinct().count() != taskIds.size())
            throw new IllegalArgumentException("Duplicate task IDs detected in the request.");

        for (UserDto u : request.users()) {
            if (u.availabilities() != null)
                for (UserDto.WorkerAvailabilityDto a : u.availabilities())
                    if (a.startTime() != null && a.endTime() != null && !a.startTime().isBefore(a.endTime()))
                        throw new IllegalArgumentException("User [id=" + u.id() + "] has an availability window where startTime ["
                                + a.startTime() + "] is not before endTime [" + a.endTime() + "].");

            if (u.maxTasks() < 1 || u.maxTasks() > 100)
                throw new IllegalArgumentException("User [id=" + u.id() + "] has invalid maxTasks: "
                        + u.maxTasks() + ". Must be between 1 and 100.");

            if (u.vacations() != null)
                for (VacationDto v : u.vacations())
                    if (v.startDate() != null && v.endDate() != null && v.startDate().isAfter(v.endDate()))
                        throw new IllegalArgumentException("User [id=" + u.id() + "] has a vacation where startDate ["
                                + v.startDate() + "] is after endDate [" + v.endDate() + "].");
        }

        for (TaskDto t : request.tasks()) {
            if (t.durationHours() <= 0)
                throw new IllegalArgumentException("Task [id=" + t.id() + "] has non-positive durationHours: " + t.durationHours());

            if (t.durationHours() > 8760)
                throw new IllegalArgumentException("Task [id=" + t.id() + "] durationHours exceeds 8760 (one year): " + t.durationHours());

            if (t.priorityLevel() != null && t.priorityLevel() < 0)
                throw new IllegalArgumentException("Task [id=" + t.id() + "] has a negative priorityLevel: " + t.priorityLevel());

            if (t.constraints() != null) {
                for (TaskConstraintDto constraint : t.constraints()) {
                    Long predId = constraint.predecessorId();

                    if (predId == null) continue;

                    if (!taskIds.contains(predId))
                        throw new IllegalArgumentException("Task [id=" + t.id() + "] references unknown predecessor task id: " + predId);

                    if (predId.equals(t.id()))
                        throw new IllegalArgumentException("Task [id=" + t.id() + "] lists itself as a predecessor (circular reference).");
                }
            }
        }

        log.debug("[AlgoService] Sanitization passed for {} user(s) and {} task(s)",
                request.users().size(), request.tasks().size());
    }

    // ─── Step 3: Strategy selection ──────────────────────────────────────────

    private SchedulingStrategy resolveStrategy(String strategyName,
                                               com.example.algorithm.model.AlgoSchedulingConfiguration config) {
        String name = (strategyName != null) ? strategyName.toUpperCase().trim() : DEFAULT_STRATEGY;
        return switch (name) {
            case "MEMETIC" -> new MemeticSchedulingStrategy(
                                      config != null ? config : new com.example.algorithm.model.AlgoSchedulingConfiguration(
                                              1.0, 1.0, 1.0, 50, 100, 0.1, 0.9, 0.2));
            case "ROUND_ROBIN" -> new RoundRobinSchedulingStrategy();
            case "CONSTRAINT_PROGRAMMING" -> new ConstraintProgrammingStrategy();
            default -> new GreedySchedulingStrategy();
        };
    }

    // ─── Step 4: Model → Response DTO mapping ────────────────────────────────

    private SchedulingResponseDto buildResponse(SchedulingStrategy strategy, int totalTasks,
                                                List<TaskAssignment> assignments) {
        List<AssignmentDto> dtos = new ArrayList<>(assignments.size());
        List<SchedulingResponseDto.UnscheduledTaskDto> unscheduled = new ArrayList<>();
        int assigned = 0;

        for (TaskAssignment a : assignments) {
            boolean isAssigned = a.getAssignedEmployee() != null
                    && a.getAssignedEmployee().getId() != null
                    && a.getScheduledStart() != null
                    && a.getScheduledEnd() != null;

            if (isAssigned) {
                assigned++;
                dtos.add(new AssignmentDto(
                        a.getTask().getId(),
                        a.getAssignedEmployee().getId(),
                        a.getScheduledStart(),
                        a.getScheduledEnd(),
                        a.getReason()
                ));
            } else {
                unscheduled.add(new SchedulingResponseDto.UnscheduledTaskDto(
                        a.getTask().getId(),
                        (a.getReason() != null && !a.getReason().isEmpty()) ? a.getReason() : "Failed to assign valid constraints"
                ));
            }
        }

        if (strategy instanceof MemeticSchedulingStrategy memetic) {
            List<Double> fitnessHistory = new ArrayList<>(memetic.getFitnessHistory());
            return new MemeticScheduleResponseDto(strategy.getName(), totalTasks, assigned, totalTasks - assigned, dtos, unscheduled, fitnessHistory);
        } else
            return new SchedulingResponseDto(strategy.getName(), totalTasks, assigned, totalTasks - assigned, dtos, unscheduled);
    }
}
