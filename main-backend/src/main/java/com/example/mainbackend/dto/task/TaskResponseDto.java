package com.example.mainbackend.dto.task;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Integer durationHours;
    private LocalDateTime startTime;

    // Priority metadata
    private Long priorityId;
    private String priorityName;

    // Task lifecycle status (OPEN, LOCKED, CLOSED) — from task_statuses table
    private Long taskStatusId;
    private String taskStatusName;
    private String taskStatusColorCode;

    /** Name of the department this task belongs to (null if unassigned). */
    private String departmentName;

    /**
     * Optimistic Locking Version.
     * Sent to frontend so it can be returned during saves to prevent stale updates.
     */
    private Long version;

    /**
     * Role requirements for the task.
     */
    private java.util.Set<Long> requiredRoleIds;
}
