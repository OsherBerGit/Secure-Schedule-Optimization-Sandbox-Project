package com.example.mainbackend.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskCreateRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private LocalDateTime deadline;

    @Positive(message = "Duration must be a positive number")
    private Integer durationHours;

    // IDs for related entities
    @NotNull(message = "Priority is required")
    private Long priorityId;

    @NotNull(message = "Status is required")
    private Long statusId;
    // assignedWorkerId removed — use POST /api/settlements to assign a worker to a task
}
