package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Task data sent to the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoTaskRequest {
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

