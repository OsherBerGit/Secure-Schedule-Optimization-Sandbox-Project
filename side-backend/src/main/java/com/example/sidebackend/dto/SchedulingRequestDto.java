package com.example.sidebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

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
