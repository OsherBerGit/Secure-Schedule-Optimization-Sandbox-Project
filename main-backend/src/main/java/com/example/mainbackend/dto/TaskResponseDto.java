package com.example.mainbackend.dto;

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
    private Double durationHours;

    // IDs for related entities
    private Long priorityId;
    private Long statusId;
    private Long assignedWorkerId; // Can be null if not yet assigned

    // Display names for easy UI rendering
    private String priorityName;
    private String statusName;
    private String assignedWorkerName;
}
