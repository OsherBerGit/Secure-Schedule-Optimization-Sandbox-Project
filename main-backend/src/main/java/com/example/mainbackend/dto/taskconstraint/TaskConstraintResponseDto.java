package com.example.mainbackend.dto.taskconstraint;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConstraintResponseDto {
    private Long id;
    private Long predecessorTaskId;
    private Long successorTaskId;
    private Long constraintTypeId;
    private Integer lagMinutes;

    // Display names for UI
    private String predecessorTaskTitle;
    private String successorTaskTitle;
    private String constraintTypeName;
}
