package com.example.mainbackend.dto.taskconstraint;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConstraintCreateRequest {
    @NotNull(message = "Predecessor task ID is required")
    private Long predecessorTaskId;

    @NotNull(message = "Successor task ID is required")
    private Long successorTaskId;

    @NotNull(message = "Constraint type ID is required")
    private Long constraintTypeId;

    private Integer lagMinutes;
}
