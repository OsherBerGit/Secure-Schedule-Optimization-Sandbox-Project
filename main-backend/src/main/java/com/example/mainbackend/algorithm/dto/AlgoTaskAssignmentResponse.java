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
    private Long assignedUserId;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
}

