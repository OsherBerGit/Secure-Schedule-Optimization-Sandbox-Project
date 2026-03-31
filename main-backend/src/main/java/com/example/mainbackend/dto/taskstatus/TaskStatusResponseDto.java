package com.example.mainbackend.dto.taskstatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskStatusResponseDto {
    private Long id;
    private String name;
}

