package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.taskpriority.TaskPriorityResponseDto;
import com.example.mainbackend.entity.TaskPriority;
import org.springframework.stereotype.Component;

@Component
public class TaskPriorityMapper {

    public TaskPriorityResponseDto toDto(TaskPriority priority) {
        if (priority == null) return null;

        return TaskPriorityResponseDto.builder()
                .id(priority.getId())
                .name(priority.getName())
                .build();
    }
}
