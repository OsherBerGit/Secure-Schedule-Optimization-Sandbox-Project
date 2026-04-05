package com.example.mainbackend.dto.task;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 hour")
    private Integer durationHours;

    /**
     * Optimistic Locking Version.
     * Required for updates to prevent concurrent modification.
     */
    private Long version;

    // IDs for related entities
    @NotNull(message = "Priority is required")
    private Long priorityId;

    private Long statusId;

    /**
     * ID of the Skill required to perform this task.
     * Optional. If missing, any worker can potentially perform it.
     */
    private Set<Long> requiredSkills;

    /**
     * Optional Department ID for assigning the task to a specific department.
     * Required for ADMIN when creating department-scoped tasks.
     * MANAGERs must only use their own department ID (or leave it blank to auto-assign).
     */
    private Long departmentId;
}
