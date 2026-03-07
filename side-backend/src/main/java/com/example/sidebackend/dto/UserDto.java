package com.example.sidebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Anonymous worker DTO — Zero-Trust contract.
 *
 * <p>No names, emails, or personal identifiers are included.
 * The algorithm engine only needs capacity and role data.</p>
 *
 * @param id                       Internal database ID of the worker
 * @param dailyAvailabilityHours   Hours per day the worker is available (1–24)
 * @param maxTasks                 Maximum concurrent tasks the worker can handle (1–100)
 * @param roleIds                  Set of role IDs the worker holds
 * @param vacations                Approved vacation windows (may be empty, never null)
 */
public record UserDto(

        @NotNull(message = "User id must not be null")
        Long id,

        @NotNull(message = "dailyAvailabilityHours must not be null")
        Integer dailyAvailabilityHours,

        @NotNull(message = "maxTasks must not be null")
        Integer maxTasks,

        Set<Long> roleIds,

        @Valid
        List<VacationDto> vacations

) {}

