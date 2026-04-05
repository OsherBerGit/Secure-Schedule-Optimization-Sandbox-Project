package com.example.mainbackend.dto.taskpriority;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPriorityResponseDto {
    private Long id;
    private String name;
}
