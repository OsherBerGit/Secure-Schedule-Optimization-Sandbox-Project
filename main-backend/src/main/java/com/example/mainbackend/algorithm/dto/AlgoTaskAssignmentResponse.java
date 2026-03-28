package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.time.LocalDateTime;

/** A single task assignment result returned by the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoTaskAssignmentResponse {
    private Long taskId;
    private String taskTitle;
    private Long assignedUserId;
    private String assignedUserFullName;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private String reason;
}

