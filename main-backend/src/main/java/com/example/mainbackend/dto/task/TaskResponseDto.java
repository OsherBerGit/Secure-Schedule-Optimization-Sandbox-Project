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
}
