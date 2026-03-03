package com.example.algorithm.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Represents a task sent from main-backend for scheduling. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskRequest {
    private Long id;
    private String title;
    private String description;
    private Integer durationHours;
    private LocalDateTime deadline;
    private String priority;
    private Integer priorityLevel;
    private String status;
    private Set<String> requiredRoles;
    private List<Long> predecessorTaskIds;
    private List<Long> successorTaskIds;
}

