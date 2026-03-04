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

    // IDs for related entities
    private Long priorityId;
    private Long statusId;

    // Display names for easy UI rendering
    private String priorityName;
    private String statusName;
    // Assignment is now managed exclusively via Settlement.
    // To see who is assigned, query GET /api/settlements/task/{taskId}
}
