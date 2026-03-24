package com.example.sidebackend.service;

import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.engine.GreedySchedulingStrategy;
import com.example.algorithm.engine.ConstraintProgrammingStrategy;
import com.example.algorithm.engine.MemeticSchedulingStrategy;
import com.example.algorithm.engine.RoundRobinSchedulingStrategy;
import com.example.algorithm.engine.Scheduler;
import com.example.algorithm.engine.SchedulingStrategy;
import com.example.algorithm.model.TaskAssignment;
import com.example.sidebackend.dto.SchedulingRequestDto;
import com.example.sidebackend.dto.SchedulingResponseDto;
import com.example.sidebackend.dto.SchedulingResponseDto.AssignmentDto;
import com.example.sidebackend.dto.TaskDto;
import com.example.sidebackend.dto.UserDto;
import com.example.sidebackend.dto.VacationDto;
import com.example.sidebackend.service.AlgoMapper.MappedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AlgoService — Sandbox Gatekeeper bridge layer.
 *
 * <p>Responsibilities (in order):</p>
 * <ol>
 *   <li><b>Sanitize</b>  — validate business-level invariants that {@code @Valid} cannot catch.</li>
 *   <li><b>Map</b>       — convert side-backend DTOs to algorithm internal models.</li>
 *   <li><b>Execute</b>   — instantiate the requested {@link SchedulingStrategy} and run it.</li>
 *   <li><b>Map back</b>  — convert {@link TaskAssignment} results to outbound DTOs.</li>
 * </ol>
 *
 * <p>This service is completely stateless — no DB, no HTTP, no file I/O.</p>
 */
@Service
public class AlgoService {

    private static final Logger log = LoggerFactory.getLogger(AlgoService.class);
    private static final String DEFAULT_STRATEGY = "GREEDY";

    // ─── Public entry point ───────────────────────────────────────────────────

    /**
     * Validates, maps and runs the scheduling algorithm for the given request.
     *
     * @param request fully-validated inbound DTO (already passed {@code @Valid})
     * @return scheduling results wrapped in a response DTO
     * @throws IllegalArgumentException if sanitization detects unsafe/invalid data
     */
    public SchedulingResponseDto schedule(SchedulingRequestDto request) {

        // 1. Sanitization — business-level safety checks
        sanitize(request);

        // 2. Map DTOs → algorithm models (via AlgoMapper)
        MappedRequest mapped = AlgoMapper.toModels(request);
        ScheduleData data = new ScheduleData(mapped.users(), mapped.tasks());

        // 3. Select and execute strategy
        SchedulingStrategy strategy = resolveStrategy(request.strategy(), mapped.config());
        log.info("[AlgoService] Running strategy '{}' with {} user(s) and {} task(s)",
                strategy.getName(), mapped.users().size(), mapped.tasks().size());

        Scheduler scheduler = new Scheduler(strategy);
        List<TaskAssignment> assignments = scheduler.run(data);

        // 4. Map results → response DTO (pass strategy so fitnessHistory can be extracted)
        return buildResponse(strategy, mapped.tasks().size(), assignments);
    }

    // ─── Step 1: Sanitization ─────────────────────────────────────────────────

    /**
     * Performs deep business-level sanitization before data reaches the algorithm.
     *
     * <p>Checks include:</p>
     * <ul>
     *   <li>No duplicate worker or task IDs.</li>
     *   <li>Each availability window has startTime strictly before endTime.</li>
     *   <li>maxTasks is within sensible bounds (1–100).</li>
     *   <li>durationHours is positive and does not exceed 8760 (one year in hours).</li>
     *   <li>priorityLevel, if present, is non-negative.</li>
     *   <li>Vacation dates: startDate must not be after endDate.</li>
     *   <li>Predecessor task IDs must reference existing task IDs (no dangling refs, no self-loops).</li>
     * </ul>
     *
     * @throws IllegalArgumentException with a descriptive message on first violation found
     */
    private void sanitize(SchedulingRequestDto request) {

        // ── User ID uniqueness ────────────────────────────────────────────────
        List<Long> userIds = request.users().stream().map(UserDto::id).toList();
        if (userIds.stream().distinct().count() != userIds.size())
            throw new IllegalArgumentException("Duplicate user IDs detected in the request.");

        // ── Task ID uniqueness ────────────────────────────────────────────────
        List<Long> taskIds = request.tasks().stream().map(TaskDto::id).toList();
        if (taskIds.stream().distinct().count() != taskIds.size())
            throw new IllegalArgumentException("Duplicate task IDs detected in the request.");

        // ── User field bounds ─────────────────────────────────────────────────
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

        // ── Task field bounds ─────────────────────────────────────────────────
        for (TaskDto t : request.tasks()) {
            if (t.durationHours() <= 0)
                throw new IllegalArgumentException(
                        "Task [id=" + t.id() + "] has non-positive durationHours: " + t.durationHours());

            if (t.durationHours() > 8760)
                throw new IllegalArgumentException(
                        "Task [id=" + t.id() + "] durationHours exceeds 8760 (one year): " + t.durationHours());

            if (t.priorityLevel() != null && t.priorityLevel() < 0)
                throw new IllegalArgumentException(
                        "Task [id=" + t.id() + "] has a negative priorityLevel: " + t.priorityLevel());

            if (t.predecessorTaskIds() != null) {
                for (Long predId : t.predecessorTaskIds()) {
                    if (!taskIds.contains(predId))
                        throw new IllegalArgumentException(
                                "Task [id=" + t.id() + "] references unknown predecessor task id: " + predId);
                    if (predId.equals(t.id()))
                        throw new IllegalArgumentException(
                                "Task [id=" + t.id() + "] lists itself as a predecessor (circular reference).");
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
            case "MEMETIC"     -> MemeticSchedulingStrategy.withDefaults(
                                      config != null ? config : new com.example.algorithm.model.AlgoSchedulingConfiguration(
                                              1.0, 1.0, 1.0, 50, 100, 0.1, 0.9, 0.2));
            case "ROUND_ROBIN" -> new RoundRobinSchedulingStrategy();
            case "CONSTRAINT_PROGRAMMING" -> new ConstraintProgrammingStrategy();
            default            -> new GreedySchedulingStrategy();
        };
    }

    // ─── Step 4: Model → Response DTO mapping ────────────────────────────────

    /**
     * Maps TaskAssignment results to the anonymous outbound response DTO.
     * Extracts fitnessHistory from MemeticSchedulingStrategy if applicable.
     * Zero-Trust: no names or task titles are included in the response — only IDs and reasons.
     */
    private SchedulingResponseDto buildResponse(SchedulingStrategy strategy, int totalTasks,
                                                List<TaskAssignment> assignments) {
        List<AssignmentDto> dtos = new ArrayList<>(assignments.size());
        List<SchedulingResponseDto.UnscheduledTaskDto> unscheduled = new ArrayList<>();
        int assigned = 0;

        for (TaskAssignment a : assignments) {
            boolean isAssigned = (a.getAssignedEmployee() != null);
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
                        a.getReason()
                ));
            }
        }

        // Extract convergence data — only MemeticSchedulingStrategy populates this
        List<Double> fitnessHistory = (strategy instanceof MemeticSchedulingStrategy memetic)
                ? new ArrayList<>(memetic.getFitnessHistory())
                : null;

        return new SchedulingResponseDto(
                strategy.getName(), totalTasks, assigned, totalTasks - assigned,
                dtos, unscheduled, fitnessHistory);
    }
}

