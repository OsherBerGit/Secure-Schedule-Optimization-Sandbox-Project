package com.example.sidebackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Anonymous task DTO — Zero-Trust contract.
 *
 * <p>No titles, descriptions, or any human-readable text is included.
 * The algorithm engine only needs scheduling-relevant numeric and temporal data.</p>
 *
 * @param id                   Internal database ID of the task
 * @param durationHours        Estimated hours required to complete the task (must be > 0)
 * @param deadline             Hard deadline by which the task must be completed
 * @param priorityLevel        Numeric priority value (higher = more urgent); may be null
 * @param requiredSkillIds     Skill IDs that a worker must hold to be eligible
 * @param constraints          IDs of tasks that must be completed before this one starts
 */
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
