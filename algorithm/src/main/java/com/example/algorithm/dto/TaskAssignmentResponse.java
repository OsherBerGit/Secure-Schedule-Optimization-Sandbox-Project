package com.example.algorithm.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a single task assignment result returned to main-backend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentResponse {

    private Long taskId;
    private String taskTitle;

    /** Null if no eligible employee was found */
    private Long assignedUserId;
    private String assignedUserFullName;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;

    /** Human-readable explanation of why this employee was chosen */
    private String reason;
}

