package com.example.algorithm.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents the result of assigning one task to one employee.
 * Produced by a {@link com.example.algorithm.engine.SchedulingStrategy}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TaskAssignment {

    private AlgoTask task;
    private AlgoUser assignedEmployee;

    /** Calculated start time for this task */
    private LocalDateTime scheduledStart;

    /** Calculated end time (scheduledStart + durationHours) */
    private LocalDateTime scheduledEnd;

    /** Human-readable reason why this employee was chosen */
    private String reason;
}

