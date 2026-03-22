package com.example.mainbackend.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private LocalDateTime deadline;

    @Positive(message = "Duration must be a positive number")
    private Integer durationHours;

    /**
     * Optimistic Locking Version.
     * Required for updates to prevent concurrent modification.
     */
    private Long version;

    // IDs for related entities
    @NotNull(message = "Priority is required")
    private Long priorityId;
    // statusId removed — status is now tracked on Settlement, not Task
    // assignedWorkerId removed — use POST /api/settlements to assign a worker to a task

    /**
     * Set of Role IDs that are required for a worker to perform this task.
     * Can be empty if no specific roles are required.
     */
    private java.util.Set<Long> requiredRoleIds;
}
