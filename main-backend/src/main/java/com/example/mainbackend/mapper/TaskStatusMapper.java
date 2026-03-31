package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.taskstatus.TaskStatusResponseDto;
import com.example.mainbackend.entity.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusMapper {

    public TaskStatusResponseDto toDto(TaskStatus status) {
        if (status == null) return null;

        return TaskStatusResponseDto.builder()
                .id(status.getId())
                .name(status.getName())
                .build();
    }
}
