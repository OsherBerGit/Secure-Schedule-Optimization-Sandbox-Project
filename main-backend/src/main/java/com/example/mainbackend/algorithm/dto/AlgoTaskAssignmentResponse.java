package com.example.mainbackend.algorithm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.time.LocalDateTime;

/** A single task assignment result returned by the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoTaskAssignmentResponse {
    @NotNull(message = "Task ID must not be null")
    @Positive
    private Long taskId;

    private String taskTitle;

    @NotNull(message = "Assigned User ID must not be null")
    @Positive
    private Long assignedUserId;

    private String assignedUserFullName;

    @NotNull(message = "End time is required")
    private LocalDateTime scheduledStart;

    @NotNull(message = "End time is required")
    private LocalDateTime scheduledEnd;

    private String reason;
}
