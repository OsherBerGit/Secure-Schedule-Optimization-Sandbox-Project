package com.example.sidebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Anonymous worker DTO — Zero-Trust contract.
 *
 * <p>No names, emails, or personal identifiers are included.
 * The algorithm engine only needs capacity and role data.</p>
 *
 * @param id             Internal database ID of the worker
 * @param availabilities Specific weekly shift windows defining when the worker is available
 * @param maxTasks       Maximum concurrent tasks the worker can handle (1–100)
 * @param jobIds        Set of job IDs the worker holds
 * @param vacations      Approved vacation windows (may be empty, never null)
 */
public record UserDto(

        @NotNull(message = "User id must not be null")
        Long id,

        @Valid
        List<WorkerAvailabilityDto> availabilities,

        @NotNull(message = "maxTasks must not be null")
        Integer maxTasks,

        Set<Long> jobIds,

        @Valid
        List<VacationDto> vacations

) {
    /** Inlined DTO for a single weekly availability window. */
    public record WorkerAvailabilityDto(
            Long id,
            @NotNull(message = "dayOfWeek must not be null") DayOfWeek dayOfWeek,
            @NotNull(message = "startTime must not be null") LocalTime startTime,
            @NotNull(message = "endTime must not be null")   LocalTime endTime
    ) {}
}

