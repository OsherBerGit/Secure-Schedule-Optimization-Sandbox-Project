package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.util.List;

/** Top-level response from the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoScheduleResponse {
    private String strategyUsed;
    private int totalTasks;
    private int assignedTasks;
    private int unassignedTasks;
    private List<AlgoTaskAssignmentResponse> assignments;
}

