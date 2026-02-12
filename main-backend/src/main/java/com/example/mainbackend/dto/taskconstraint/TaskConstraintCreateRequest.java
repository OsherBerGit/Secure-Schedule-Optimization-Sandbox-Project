package com.example.mainbackend.dto.taskconstraint;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskConstraintCreateRequest {
    @NotNull(message = "Predecessor task ID is required")
    private Long predecessorTaskId;

    @NotNull(message = "Successor task ID is required")
    private Long successorTaskId;

    @NotNull(message = "Constraint type ID is required")
    private Long constraintTypeId;

    // Lag time in minutes (default 0 if not provided)
    private Integer lagMinutes;
}
