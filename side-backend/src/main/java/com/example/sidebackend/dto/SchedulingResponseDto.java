package com.example.sidebackend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Top-level outbound DTO returned by POST /api/v1/algo/schedule.
 *
 * <p>Zero-Trust contract: IDs and scheduling timestamps only.
 * {@code unscheduledTasks} carries the failure reason so callers can surface
 * explainability information to end-users.</p>
 *
 * @param strategyUsed      Name of the strategy that ran
 * @param totalTasks        Total number of tasks that were processed
 * @param assignedTasks     Number of tasks successfully assigned to a worker
 * @param unassignedTasks   Number of tasks that could not be assigned
 * @param assignments       Per-task assignment details (assigned tasks only)
 * @param unscheduledTasks  Tasks that the algorithm could not assign, with reasons
 */
public record SchedulingResponseDto(

        String strategyUsed,
        int totalTasks,
        int assignedTasks,
        int unassignedTasks,
        List<AssignmentDto> assignments,
        List<UnscheduledTaskDto> unscheduledTasks,
        /** Best fitness score per generation — populated only for the MEMETIC strategy. */
        List<Double> fitnessHistory

) {

    /**
     * Anonymous result record for a single task assignment.
     *
     * @param taskId           ID of the task
     * @param assignedUserId   ID of the assigned worker (null if unassigned)
     * @param scheduledStart   Calculated start time
     * @param scheduledEnd     Calculated end time
     * @param reason           Scheduling decision explanation
     */
    public record AssignmentDto(
            Long taskId,
            Long assignedUserId,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd,
            String reason
    ) {}

    /**
     * A task that could not be assigned to any worker, with a failure reason.
     *
     * <p>Zero-Trust: only the task ID is stored here. The human-readable name
     * is injected by the main-backend enrichment layer after it receives this DTO.</p>
     *
     * @param taskId  ID of the unscheduled task
     * @param reason  Human-readable explanation of why scheduling failed
     */
    public record UnscheduledTaskDto(
            Long   taskId,
            String reason
    ) {}
}
