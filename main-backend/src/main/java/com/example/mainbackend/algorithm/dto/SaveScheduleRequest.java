package com.example.mainbackend.algorithm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request body for {@code POST /api/schedule/save}.
 *
 * <p>The frontend sends back the draft assignments that the user has approved,
 * and this service persists them: Task → SCHEDULED, Settlement → ASSIGNED.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveScheduleRequest {

    /**
     * The list of approved assignments to persist.
     * Only assigned tasks (assignedUserId != null) will be acted upon.
     */
    @NotNull(message = "assignments must not be null")
    @Valid
    private List<TaskAssignmentDto> assignments;

    // ── Nested DTO ────────────────────────────────────────────────────────────

    /**
     * A single (task → worker) assignment from the draft schedule.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskAssignmentDto {

        @NotNull(message = "taskId must not be null")
        private Long taskId;

        /** Null means the task could not be assigned — skip silently. */
        private Long assignedUserId;

        /** Proposed start time returned by the algorithm (may be null). */
        private LocalDateTime scheduledStart;

        /** Proposed end time returned by the algorithm (may be null). */
        private LocalDateTime scheduledEnd;

        /**
         * The version of the Task entity when it was fetched.
         * Used for optimistic locking to prevent concurrent modification.
         */
        @NotNull(message = "Task version is required for optimistic locking")
        private Long version;
    }
}
