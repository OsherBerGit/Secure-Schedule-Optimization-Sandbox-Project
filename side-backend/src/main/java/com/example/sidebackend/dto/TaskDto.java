package com.example.sidebackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record TaskDto(

        @NotNull(message = "Task id must not be null")
        Long id,

        @NotNull(message = "durationHours must not be null")
        Integer durationHours,

        LocalDateTime deadline,

        Integer priorityLevel,

        Set<Long> requiredSkillIds,

        List<TaskConstraintDto> constraints) {
}
