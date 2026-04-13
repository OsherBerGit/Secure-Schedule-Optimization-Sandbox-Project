package com.example.mainbackend.algorithm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveScheduleRequest {

    @NotNull(message = "assignments must not be null")
    @NotEmpty(message = "Assignments list must not be empty")
    @Valid
    private List<TaskAssignmentDto> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskAssignmentDto {

        @NotNull(message = "taskId must not be null")
        private Long taskId;

        @Positive(message = "Assigned User ID must be positive")
        private Long assignedUserId;

        private LocalDateTime scheduledStart;

        private LocalDateTime scheduledEnd;

        @NotNull(message = "Task version is required for optimistic locking")
        @Min(0)
        private Long version;
    }
}
