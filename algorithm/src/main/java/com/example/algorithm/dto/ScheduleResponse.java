package com.example.algorithm.dto;

import lombok.*;

import java.util.List;

/**
 * The top-level response body returned from POST /api/v1/algo/schedule.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    /** Name of the strategy that was used */
    private String strategyUsed;

    /** Total number of tasks processed */
    private int totalTasks;

    /** Number of tasks that were successfully assigned */
    private int assignedTasks;

    /** Number of tasks that could not be assigned (no eligible employee) */
    private int unassignedTasks;

    /** The list of individual task assignments */
    private List<TaskAssignmentResponse> assignments;
}

