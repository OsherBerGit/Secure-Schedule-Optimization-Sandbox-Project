package com.example.mainbackend.dto.task;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskCreateRequest {
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Integer durationHours;

    // IDs for related entities
    private Long priorityId;
    private Long statusId;
    private Long assignedWorkerId; // Can be null if not yet assigned
}
