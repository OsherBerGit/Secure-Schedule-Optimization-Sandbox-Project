package com.example.mainbackend.dto.taskpriority;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskPriorityResponseDto {
    private Long id;
    private String name;
}

