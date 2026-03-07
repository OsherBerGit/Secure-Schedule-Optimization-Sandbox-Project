package com.example.sidebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Top-level inbound DTO for POST /api/v1/algo/schedule.
 *
 * <p>Zero-Trust contract: carries only anonymous IDs, numeric capacities,
 * and dates. No names, emails, titles, or descriptions are accepted.</p>
 *
 * @param strategy  Scheduling strategy: "GREEDY" (default) or "ROUND_ROBIN"
 * @param config    Active scheduling configuration (weights, GA params)
 * @param users     Workers available for assignment
 * @param tasks     Tasks to be scheduled
 */
public record SchedulingRequestDto(

        String strategy,

        @Valid
        SchedulingConfigurationDto config,

        @NotNull(message = "Users list must not be null")
        @NotEmpty(message = "At least one user is required")
        @Valid
        List<UserDto> users,

        @NotNull(message = "Tasks list must not be null")
        @NotEmpty(message = "At least one task is required")
        @Valid
        List<TaskDto> tasks

) {}
