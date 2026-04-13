package com.example.sidebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public record UserDto(

        @NotNull(message = "User id must not be null")
        Long id,

        @Valid
        List<WorkerAvailabilityDto> availabilities,

        @NotNull(message = "maxTasks must not be null")
        Integer maxTasks,

        Set<Long> skillIds,

        @Valid
        List<VacationDto> vacations

) {
    public record WorkerAvailabilityDto(
            Long id,
            @NotNull(message = "dayOfWeek must not be null") DayOfWeek dayOfWeek,
            @NotNull(message = "startTime must not be null") LocalTime startTime,
            @NotNull(message = "endTime must not be null")   LocalTime endTime
    ) {}
}

