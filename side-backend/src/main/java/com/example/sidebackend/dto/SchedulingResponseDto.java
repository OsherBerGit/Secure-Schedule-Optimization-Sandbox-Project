package com.example.sidebackend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Top-level outbound DTO returned by POST /api/v1/algo/schedule.
 *
 * <p>Contains scheduling statistics and the full list of individual
 * task assignments produced by the selected strategy.</p>
 *
 * @param strategyUsed   Human-readable name of the strategy that ran
 * @param totalTasks     Total number of tasks that were processed
 * @param assignedTasks  Number of tasks successfully assigned to a worker
 * @param unassignedTasks Number of tasks that could not be assigned
 * @param assignments    Per-task assignment details
 */
public record SchedulingResponseDto(

        String strategyUsed,
        int totalTasks,
        int assignedTasks,
        int unassignedTasks,
        List<AssignmentDto> assignments

) {

    /**
     * Result record for a single task assignment.
     *
     * @param taskId               ID of the task
     * @param taskTitle            Title of the task
     * @param assignedUserId       ID of the assigned user (null if unassigned)
     * @param assignedUserFullName Full name of the assigned user (null if unassigned)
     * @param scheduledStart       Calculated start time
     * @param scheduledEnd         Calculated end time
     * @param reason               Human-readable explanation of the assignment decision
     */
    public record AssignmentDto(
            Long taskId,
            String taskTitle,
            Long assignedUserId,
            String assignedUserFullName,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd,
            String reason
    ) {}
}
