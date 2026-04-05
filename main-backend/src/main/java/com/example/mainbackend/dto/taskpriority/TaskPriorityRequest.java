package com.example.mainbackend.dto.taskpriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPriorityRequest {
    @NotBlank(message = "Priority name is required")
    @Size(min = 2, max = 50, message = "Priority name must be between 2 and 50 characters")
    private String name;
}
