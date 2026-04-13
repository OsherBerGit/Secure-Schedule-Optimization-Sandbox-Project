package com.example.sidebackend.dto;

import java.time.LocalDateTime;
import java.util.List;

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

    public record AssignmentDto(
            Long taskId,
            Long assignedUserId,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd,
            String reason
    ) {}

    public record UnscheduledTaskDto(
            Long   taskId,
            String reason
    ) {}
}
