package com.example.sidebackend.dto;

import java.util.List;

public class MemeticScheduleResponseDto extends SchedulingResponseDto {

    private List<Double> fitnessHistory;

    public MemeticScheduleResponseDto() { super(); }

    public MemeticScheduleResponseDto(String strategyUsed, int totalTasks, int assignedTasks,
                                      int unassignedTasks, List<SchedulingResponseDto.AssignmentDto> assignments,
                                      List<SchedulingResponseDto.UnscheduledTaskDto> unscheduledTasks,
                                      List<Double> fitnessHistory) {
        super(strategyUsed, totalTasks, assignedTasks, unassignedTasks, assignments, unscheduledTasks);
        this.fitnessHistory = fitnessHistory;
    }

    public List<Double> getFitnessHistory() { return fitnessHistory; }
    public void setFitnessHistory(List<Double> fitnessHistory) { this.fitnessHistory = fitnessHistory; }
}
