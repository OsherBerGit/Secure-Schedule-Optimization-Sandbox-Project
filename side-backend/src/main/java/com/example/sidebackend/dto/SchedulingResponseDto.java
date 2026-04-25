package com.example.sidebackend.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "strategyUsed",
        visible = true,
        defaultImpl = SchedulingResponseDto.class
)
@JsonSubTypes({ @JsonSubTypes.Type(value = MemeticScheduleResponseDto.class, name = "MEMETIC") })
public class SchedulingResponseDto {

    private String strategyUsed;
    private int totalTasks;
    private int assignedTasks;
    private int unassignedTasks;
    private List<AssignmentDto> assignments;
    private List<UnscheduledTaskDto> unscheduledTasks;

    public SchedulingResponseDto() { }

    public SchedulingResponseDto(String strategyUsed, int totalTasks, int assignedTasks,
                                 int unassignedTasks, List<AssignmentDto> assignments,
                                 List<UnscheduledTaskDto> unscheduledTasks) {
        this.strategyUsed = strategyUsed;
        this.totalTasks = totalTasks;
        this.assignedTasks = assignedTasks;
        this.unassignedTasks = unassignedTasks;
        this.assignments = assignments;
        this.unscheduledTasks = unscheduledTasks;
    }

    public String getStrategyUsed() { return strategyUsed; }
    public void setStrategyUsed(String strategyUsed) { this.strategyUsed = strategyUsed; }
    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
    public int getAssignedTasks() { return assignedTasks; }
    public void setAssignedTasks(int assignedTasks) { this.assignedTasks = assignedTasks; }
    public int getUnassignedTasks() { return unassignedTasks; }
    public void setUnassignedTasks(int unassignedTasks) { this.unassignedTasks = unassignedTasks; }
    public List<AssignmentDto> getAssignments() { return assignments; }
    public void setAssignments(List<AssignmentDto> assignments) { this.assignments = assignments; }
    public List<UnscheduledTaskDto> getUnscheduledTasks() { return unscheduledTasks; }
    public void setUnscheduledTasks(List<UnscheduledTaskDto> unscheduledTasks) { this.unscheduledTasks = unscheduledTasks; }

    public record AssignmentDto(
            Long taskId,
            Long assignedUserId,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd,
            String reason
    ) {}

    public record UnscheduledTaskDto(
            Long taskId,
            String reason
    ) {}
}
