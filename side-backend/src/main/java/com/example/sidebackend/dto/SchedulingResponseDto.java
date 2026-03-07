package com.example.sidebackend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Top-level outbound DTO returned by POST /api/v1/algo/schedule.
 *
 * <p>Zero-Trust contract: contains only anonymous IDs and scheduling timestamps.
 * No names, titles, or human-readable text are included in the response.</p>
 *
 * @param strategyUsed    Name of the strategy that ran
 * @param totalTasks      Total number of tasks that were processed
 * @param assignedTasks   Number of tasks successfully assigned to a worker
 * @param unassignedTasks Number of tasks that could not be assigned
 * @param assignments     Per-task assignment details
 */
public record SchedulingResponseDto(

        String strategyUsed,
        int totalTasks,
        int assignedTasks,
        int unassignedTasks,
        List<AssignmentDto> assignments

) {

    /**
     * Anonymous result record for a single task assignment.
     *
     * @param taskId           ID of the task
     * @param assignedUserId   ID of the assigned worker (null if unassigned)
     * @param scheduledStart   Calculated start time
     * @param scheduledEnd     Calculated end time
     */
    public record AssignmentDto(
            Long taskId,
            Long assignedUserId,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd
    ) {}
}
