package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponseDto toDto(Task task) {
        if (task == null) return null;

        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .deadline(task.getDeadline())
                .durationHours(task.getDurationHours())
                .startTime(task.getStartTime())
                // IDs
                .priorityId(task.getPriority() != null ? task.getPriority().getId() : null)
                .statusId(task.getStatus() != null ? task.getStatus().getId() : null)
                .assignedWorkerId(task.getAssignedEmployee() != null ? task.getAssignedEmployee().getId() : null)
                // Display names
                .priorityName(task.getPriority() != null ? task.getPriority().getName() : null)
                .statusName(task.getStatus() != null ? task.getStatus().getName() : null)
                .assignedWorkerName(buildWorkerName(task))
                .build();
    }

    /**
     * Helper method to build the worker's full name from User entity.
     * Handles all combinations of first name and last name being present or null.
     *
     * @param task the Task entity containing the assigned employee
     * @return the formatted full name, or null if no employee is assigned
     */
    private String buildWorkerName(Task task) {
        if (task.getAssignedEmployee() == null) return null;

        String firstName = task.getAssignedEmployee().getFirstName();
        String lastName = task.getAssignedEmployee().getLastName();

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else {
            return lastName;
        }
    }
}
