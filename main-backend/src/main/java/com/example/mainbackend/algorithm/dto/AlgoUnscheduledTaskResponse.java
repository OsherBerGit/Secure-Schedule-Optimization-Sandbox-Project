package com.example.mainbackend.algorithm.dto;

import lombok.*;

/**
 * A task that the scheduling algorithm could not assign to any worker,
 * enriched with the human-readable task name by the main-backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoUnscheduledTaskResponse {
    /** ID of the task that could not be scheduled. */
    private Long   taskId;
    /** Human-readable task name (injected by the enrichment layer). */
    private String taskName;
    /** Explanation of why the algorithm could not schedule this task. */
    private String reason;
}

