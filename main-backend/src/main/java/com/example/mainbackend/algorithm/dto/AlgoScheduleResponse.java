package com.example.mainbackend.algorithm.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "strategyUsed",
        visible = true,
        defaultImpl = AlgoScheduleResponse.class
)
@JsonSubTypes({ @JsonSubTypes.Type(value = MemeticScheduleResponse.class, name = "MEMETIC") })
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
}
