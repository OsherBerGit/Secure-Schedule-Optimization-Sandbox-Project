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
                // Display names
                .priorityName(task.getPriority() != null ? task.getPriority().getName() : null)
                .statusName(task.getStatus() != null ? task.getStatus().getName() : null)
                .build();
    }
}
