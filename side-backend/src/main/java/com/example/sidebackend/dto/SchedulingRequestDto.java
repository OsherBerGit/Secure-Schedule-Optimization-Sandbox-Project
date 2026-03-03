package com.example.sidebackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Top-level inbound DTO for the POST /api/v1/algo/schedule endpoint.
 *
 * <p>Carries a complete snapshot of users, tasks, and their constraints
 * so that the algorithm can run entirely in memory — no database access needed.</p>
 *
 * @param strategy  Scheduling strategy name: "GREEDY" (default) or "ROUND_ROBIN"
 * @param users     List of all users/workers available for assignment
 * @param tasks     List of tasks to be scheduled
 */
public record SchedulingRequestDto(

        String strategy,

        @NotNull(message = "Users list must not be null")
        @NotEmpty(message = "At least one user is required")
        List<UserDto> users,

        @NotNull(message = "Tasks list must not be null")
        @NotEmpty(message = "At least one task is required")
        List<TaskDto> tasks

) {

    // ─── Nested record: UserDto ───────────────────────────────────────────────

    /**
     * Represents a single worker/employee.
     *
     * @param id                     Unique identifier
     * @param firstName              First name
     * @param lastName               Last name
     * @param email                  Work email address
     * @param dailyAvailabilityHours Hours the worker is available per day
     * @param maxTasks               Maximum concurrent tasks allowed
     * @param roles                  Set of role names (e.g. "DEVELOPER", "MANAGER")
     * @param vacations              Approved vacation periods (non-null, may be empty)
     */
    public record UserDto(

            @NotNull(message = "User id must not be null")
            Long id,

            @NotEmpty(message = "User firstName must not be blank")
            String firstName,

            @NotEmpty(message = "User lastName must not be blank")
            String lastName,

            String email,

            @NotNull(message = "User dailyAvailabilityHours must not be null")
            Integer dailyAvailabilityHours,

            @NotNull(message = "User maxTasks must not be null")
            Integer maxTasks,

            Set<String> roles,

            List<VacationDto> vacations
    ) {}

    // ─── Nested record: VacationDto ───────────────────────────────────────────

    /**
     * Represents an approved vacation window for a user.
     *
     * @param id        Vacation record identifier
     * @param startDate First day of the vacation (inclusive)
     * @param endDate   Last day of the vacation (inclusive)
     */
    public record VacationDto(
            Long id,
            @NotNull(message = "Vacation startDate must not be null") LocalDate startDate,
            @NotNull(message = "Vacation endDate must not be null")   LocalDate endDate
    ) {}

    // ─── Nested record: TaskDto ───────────────────────────────────────────────

    /**
     * Represents a task to be scheduled.
     *
     * @param id                 Unique identifier
     * @param title              Short human-readable title
     * @param description        Detailed description (may be null)
     * @param durationHours      Estimated effort in hours
     * @param deadline           Hard deadline (must finish by)
     * @param priority           Priority label (e.g. "HIGH")
     * @param priorityLevel      Numeric priority (higher = more urgent)
     * @param status             Current status (e.g. "PENDING", "IN_PROGRESS")
     * @param requiredRoles      Roles an employee must have to be assigned
     * @param predecessorTaskIds IDs of tasks that must complete first
     * @param successorTaskIds   IDs of tasks that depend on this task
     */
    public record TaskDto(

            @NotNull(message = "Task id must not be null")
            Long id,

            @NotEmpty(message = "Task title must not be blank")
            String title,

            String description,

            @NotNull(message = "Task durationHours must not be null")
            Integer durationHours,

            LocalDateTime deadline,
            String priority,
            Integer priorityLevel,
            String status,
            Set<String> requiredRoles,
            List<Long> predecessorTaskIds,
            List<Long> successorTaskIds
    ) {}
}
