package com.example.mainbackend.algorithm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

/** Top-level response from the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoScheduleResponse {
    @NotBlank(message = "Strategy used must be reported")
    private String strategyUsed;

    @Min(0)
    private int totalTasks;

    @Min(0)
    private int assignedTasks;

    @Min(0)
    private int unassignedTasks;

    @NotNull
    @Valid
    private List<AlgoTaskAssignmentResponse> assignments;
    private List<AlgoUnscheduledTaskResponse> unscheduledTasks;

    /** Best fitness score per generation (Memetic algorithm only). */
    private List<Double> fitnessHistory;
}
